package io.github.aikobn26.teamprogressviz.shared.properties;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(
    @NotNull(message="app.frontend.base-uri must be provided") URI baseUri, 
    @NotNull(message="app.frontend.success-path must be provided") String successPath, 
    @NotNull(message="app.frontend.error-path must be provided") String errorPath
) {

    public FrontendProperties {
        if (!baseUri.isAbsolute() || baseUri.getHost() == null) {
            throw new IllegalArgumentException("app.frontend.base-uri must be an absolute URL");
        }
    }

    /**
     * ベースURLからオリジン (Scheme + Host + Port) を抽出します。
     * @return String
     */
    public String origin() {
        var port = baseUri.getPort() < 0 ? "" : ":" + baseUri.getPort();
        return baseUri.getScheme() + "://" + baseUri.getHost() + port;
    }

    /**
     * 成功時のリダイレクトURLを生成します。
     * @return String
     */
    public String successRedirectUrl() {
        return UriComponentsBuilder.fromUri(baseUri)
                .replacePath(successPath)
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    /**
     * エラーメッセージ付きのエラー時リダイレクトURLを生成します。
     * @param message クエリパラメータに含めるエラーメッセージ (エンコード前の生文字列)
     * @return String
     */
    public String errorRedirectUrl(String message) {
        return UriComponentsBuilder.fromUri(baseUri)
                .replacePath(errorPath)
                .replaceQuery(null)
                .queryParam("status", "error")
                .queryParam("message", message)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    /**
     * "status=success" クエリ付きの成功時リダイレクトURLを生成します。
     * @return String
     */
    public String successRedirectUrlWithStatus() {
        return UriComponentsBuilder.fromUri(baseUri)
                .replacePath(successPath)
                .replaceQuery(null)
                .queryParam("status", "success")
                .build()
                .toUriString();
    }
}