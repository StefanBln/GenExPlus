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

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

import java.io.IOException;

/**
 * DocMDP (Modification Detection and Prevention) support for certified PDF signatures.
 *
 * <p>Adapted from the Apache PDFBox {@code SigUtils.setMDPPermission} example (Apache License 2.0).
 * Certification locks the document after signing so content changes invalidate the signature and
 * compliant viewers restrict editing.
 */
final class PdfDocMdpSupport {

    private PdfDocMdpSupport() {}

    /**
     * Returns the DocMDP permission level ({@code 1}–{@code 3}), or {@code 0} when not certified.
     */
    static int getDocMdpPermission(PDDocument document) {
        COSDictionary permsDict = document.getDocumentCatalog().getCOSObject()
                .getCOSDictionary(COSName.PERMS);
        if (permsDict == null) {
            return 0;
        }
        COSDictionary signatureDict = permsDict.getCOSDictionary(COSName.DOCMDP);
        if (signatureDict == null) {
            return 0;
        }
        COSArray refArray = signatureDict.getCOSArray(COSName.REFERENCE);
        if (refArray == null) {
            return 0;
        }
        for (int i = 0; i < refArray.size(); ++i) {
            COSBase base = refArray.getObject(i);
            if (!(base instanceof COSDictionary sigRefDict)) {
                continue;
            }
            if (!COSName.DOCMDP.equals(sigRefDict.getDictionaryObject(COSName.TRANSFORM_METHOD))) {
                continue;
            }
            COSBase params = sigRefDict.getDictionaryObject(COSName.TRANSFORM_PARAMS);
            if (params instanceof COSDictionary transformDict) {
                int accessPermissions = transformDict.getInt(COSName.P, 2);
                if (accessPermissions < 1 || accessPermissions > 3) {
                    return 2;
                }
                return accessPermissions;
            }
        }
        return 0;
    }

    /**
     * Marks the signature as a certification signature with the given DocMDP permission level.
     *
     * @param document PDF being signed
     * @param signature signature dictionary to certify
     * @param accessPermissions {@code 1} = no changes, {@code 2} = form fill + signing,
     *                          {@code 3} = form fill, signing, and annotation
     */
    static void setDocMdpPermission(PDDocument document, PDSignature signature, int accessPermissions)
            throws IOException {
        if (accessPermissions < 1 || accessPermissions > 3) {
            throw new IllegalArgumentException("DocMDP permission must be 1, 2, or 3");
        }

        for (PDSignature existing : document.getSignatureDictionaries()) {
            if (COSName.DOC_TIME_STAMP.equals(existing.getCOSObject().getItem(COSName.TYPE))) {
                continue;
            }
            if (existing.getCOSObject().containsKey(COSName.CONTENTS)) {
                throw new IOException("Cannot certify: document already has an approval signature");
            }
        }

        COSDictionary sigDict = signature.getCOSObject();

        COSDictionary transformParameters = new COSDictionary();
        transformParameters.setItem(COSName.TYPE, COSName.TRANSFORM_PARAMS);
        transformParameters.setInt(COSName.P, accessPermissions);
        transformParameters.setName(COSName.V, "1.2");
        transformParameters.setNeedToBeUpdated(true);
        transformParameters.setDirect(true);

        COSDictionary referenceDict = new COSDictionary();
        referenceDict.setItem(COSName.TYPE, COSName.SIG_REF);
        referenceDict.setItem(COSName.TRANSFORM_METHOD, COSName.DOCMDP);
        referenceDict.setItem(COSName.DIGEST_METHOD, COSName.getPDFName("SHA256"));
        referenceDict.setItem(COSName.TRANSFORM_PARAMS, transformParameters);
        referenceDict.setNeedToBeUpdated(true);
        referenceDict.setDirect(true);

        COSArray referenceArray = new COSArray();
        referenceArray.add(referenceDict);
        sigDict.setItem(COSName.REFERENCE, referenceArray);
        referenceArray.setNeedToBeUpdated(true);
        referenceArray.setDirect(true);

        COSDictionary catalogDict = document.getDocumentCatalog().getCOSObject();
        COSDictionary permsDict = catalogDict.getCOSDictionary(COSName.PERMS);
        if (permsDict == null) {
            permsDict = new COSDictionary();
            catalogDict.setItem(COSName.PERMS, permsDict);
        }
        permsDict.setItem(COSName.DOCMDP, signature);
        catalogDict.setNeedToBeUpdated(true);
        permsDict.setNeedToBeUpdated(true);
    }
}
