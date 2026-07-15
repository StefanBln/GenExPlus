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

package io.github.stefanbln.genexplus.report;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared GreenMail setup for integration and E2E email tests.
 *
 * <p>Uses an ephemeral SMTP port to avoid clashes on shared CI runners and applies
 * fast timeouts with no retries so failures surface quickly.
 */
public final class GreenMailTestSupport {

    private GreenMailTestSupport() {}

    /** SMTP on {@code 127.0.0.1} with an OS-assigned port. */
    public static ServerSetup dynamicSmtp() {
        return new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
    }

    /** JUnit 5 extension backed by {@link #dynamicSmtp()}. */
    public static GreenMailExtension extension() {
        return new GreenMailExtension(dynamicSmtp());
    }

    /**
     * SMTP properties pointing at a running GreenMail instance.
     */
    public static Map<String, String> smtpProperties(GreenMailExtension greenMail) {
        var props = new LinkedHashMap<String, String>();
        props.put("mail.smtp.enabled", "true");
        props.put("mail.smtp.host", "127.0.0.1");
        props.put("mail.smtp.port", String.valueOf(greenMail.getSmtp().getPort()));
        props.put("mail.smtp.from", "sender@example.com");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.retry.count", "0");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        return props;
    }
}
