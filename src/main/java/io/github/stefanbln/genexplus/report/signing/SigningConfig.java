/*
 * Copyright 2026 Stefan Schuetz - Locivera - Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.stefanbln.genexplus.report.signing;

import io.github.stefanbln.genexplus.report.PathUtils;
import io.github.stefanbln.genexplus.report.config.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;

/**
 * Keystore and policy settings for PDF CMS signing.
 *
 * @param keystorePath PKCS#12 or JKS keystore file
 * @param keystoreType keystore type (usually {@code PKCS12})
 * @param alias private-key entry alias
 * @param password keystore password (cleared after use)
 * @param reason optional signature reason shown in PDF viewers
 * @param location optional signature location shown in PDF viewers
 * @param contactInfo optional contact string shown in PDF viewers
 * @param signerName optional display name override; defaults to certificate CN
 * @param certify when {@code true}, apply DocMDP certification to lock the document
 * @param docMdpPermission DocMDP level {@code 1}–{@code 3} when {@link #certify()} is true
 * @param visible when {@code true}, render a visible signature stamp on the PDF page
 * @param visiblePage page index ({@code 0}-based) or {@code -1} for the last page
 * @param visiblePrint when {@code true}, include the visible stamp when the PDF is printed
 */
public record SigningConfig(
        Path keystorePath,
        String keystoreType,
        String alias,
        char[] password,
        Optional<String> reason,
        Optional<String> location,
        Optional<String> contactInfo,
        Optional<String> signerName,
        boolean certify,
        int docMdpPermission,
        boolean visible,
        int visiblePage,
        boolean visiblePrint
) {

    private static final String KEY_REASON = "signing.reason";
    private static final String KEY_LOCATION = "signing.location";
    private static final String KEY_CONTACT = "signing.contact";
    private static final String KEY_SIGNER_NAME = "signing.signerName";

    /**
     * Builds signing configuration from application properties when signing is enabled.
     */
    public static SigningConfig from(Configuration configuration) throws SigningException {
        if (!configuration.isSigningEnabled()) {
            throw new SigningException("PDF signing is not enabled (signing.enabled=false)");
        }

        var keystorePathValue = configuration.getKeystorePath()
                .filter(path -> !path.isBlank())
                .orElseThrow(() -> new SigningException("signing.keystore.path is required when signing.enabled=true"));

        var alias = configuration.getKeystoreAlias()
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new SigningException("signing.keystore.alias is required when signing.enabled=true"));

        var passwordValue = configuration.getKeystorePassword()
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new SigningException(
                        "Keystore password is required (set signing.keystore.password or REPORT_KEYSTORE_PASSWORD)"));

        Path keystorePath;
        try {
            keystorePath = PathUtils.resolveExistingSecureFile(keystorePathValue);
        } catch (IOException e) {
            throw new SigningException("Keystore file not found or not readable: " + keystorePathValue, e);
        }

        var keystoreType = configuration.getKeystoreType();
        var reason = optionalProperty(configuration, KEY_REASON);
        var location = optionalProperty(configuration, KEY_LOCATION);
        var contactInfo = optionalProperty(configuration, KEY_CONTACT);
        var signerName = optionalProperty(configuration, KEY_SIGNER_NAME);
        var certify = configuration.isSigningCertify();
        var docMdpPermission = configuration.getSigningPermissions();
        var visible = configuration.isSigningVisible();
        var visiblePage = configuration.getSigningVisiblePage();
        var visiblePrint = configuration.isSigningVisiblePrint();
        var password = passwordValue.toCharArray();

        var config = new SigningConfig(
                keystorePath, keystoreType, alias, password,
                reason, location, contactInfo, signerName, certify, docMdpPermission,
                visible, visiblePage, visiblePrint);
        return config;
    }

    /**
     * Validates keystore access and signing certificate suitability.
     */
    public void validateKeystore() throws SigningException {
        loadPrivateKeyAndChain();
    }

    KeyMaterial loadPrivateKeyAndChain() throws SigningException {
        try {
            var keystore = KeyStore.getInstance(keystoreType);
            try (var input = Files.newInputStream(keystorePath)) {
                keystore.load(input, password);
            }

            if (!keystore.containsAlias(alias)) {
                throw new SigningException("Keystore alias not found: " + alias);
            }

            var privateKey = keystore.getKey(alias, password);
            if (!(privateKey instanceof java.security.PrivateKey signingKey)) {
                throw new SigningException("No private key for keystore alias: " + alias);
            }

            var chain = keystore.getCertificateChain(alias);
            if (chain == null || chain.length == 0) {
                throw new SigningException("No certificate chain for keystore alias: " + alias);
            }

            if (chain[0] instanceof X509Certificate x509) {
                SigningCertificateSupport.validateCertificateForPdfSigning(x509);
            }

            return new KeyMaterial(signingKey, chain);
        } catch (SigningException e) {
            throw e;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException
                 | UnrecoverableKeyException e) {
            throw new SigningException("Failed to load signing keystore: " + e.getMessage(), e);
        }
    }

  /**
   * Returns a validation error message for preflight checks, or {@code null} when valid.
   */
    public static String validateForReporting(Configuration configuration) {
        if (!configuration.isSigningEnabled()) {
            return null;
        }
        SigningConfig config = null;
        try {
            config = from(configuration);
            config.validateKeystore();
            return null;
        } catch (SigningException e) {
            return e.getMessage();
        } finally {
            if (config != null) {
                config.clearPassword();
            }
        }
    }

    /** Clears the in-memory keystore password. */
    public void clearPassword() {
        Arrays.fill(password, '\0');
    }

    private static Optional<String> optionalProperty(Configuration configuration, String key) {
        return Optional.ofNullable(configuration.getString(key)).filter(value -> !value.isBlank());
    }

    record KeyMaterial(java.security.PrivateKey privateKey, Certificate[] certificateChain) {}
}
