package io.github.aikobn26.teamprogressviz.shared.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

import io.github.aikobn26.teamprogressviz.auth.properties.FrontendProperties;

class FrontendPropertiesTest {

    private final FrontendProperties properties = new FrontendProperties(
            URI.create("https://frontend.example.com:8443/app"),
            "/auth/success",
            "/auth/error");

    @Test
    void origin_returnsSchemeHostAndPort() {
        assertThat(properties.origin()).isEqualTo("https://frontend.example.com:8443");
    }

    @Test
    void successRedirectUrl_replacesPath() {
        assertThat(properties.successRedirectUrl())
                .isEqualTo("https://frontend.example.com:8443/auth/success");
    }

    @Test
    void successRedirectUrlWithStatus_includesQueryParameter() {
        assertThat(properties.successRedirectUrlWithStatus())
                .isEqualTo("https://frontend.example.com:8443/auth/success?status=success");
    }

    @Test
    void errorRedirectUrl_encodesMessage() {
        assertThat(properties.errorRedirectUrl("認証に失敗しました"))
                .isEqualTo("https://frontend.example.com:8443/auth/error?status=error&message=%E8%AA%8D%E8%A8%BC%E3%81%AB%E5%A4%B1%E6%95%97%E3%81%97%E3%81%BE%E3%81%97%E3%81%9F");
    }

    @Test
    void constructor_rejectsRelativeUri() {
        assertThatThrownBy(() -> new FrontendProperties(URI.create("/relative"), "/s", "/e"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be an absolute URL");
    }
}
