package nz.co.kehrbusch.ms365.util;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.microsoft.kiota.authentication.AccessTokenProvider;
import com.microsoft.kiota.authentication.AllowedHostsValidator;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AzureIdentityAccessTokenProvider implements AccessTokenProvider {
    private final TokenCredential creds;
    private final List<String> _scopes;
    private final AllowedHostsValidator _hostValidator;
    private static final HashSet<String> localhostStrings = new HashSet(Arrays.asList("localhost", "[::1]", "::1", "127.0.0.1"));
    private static final String ClaimsKey = "claims";
    private static final String parentSpanKey = "parent-span";

    private static final String tracerInstrumentationName = "com.microsoft.kiota.authentication:microsoft-kiota-authentication-azure";

    public AzureIdentityAccessTokenProvider(@Nonnull final TokenCredential tokenCredential, @Nonnull final String[] allowedHosts,  @Nonnull final String... scopes) {
        this.creds = (TokenCredential)Objects.requireNonNull(tokenCredential, "parameter tokenCredential cannot be null");
        if (scopes == null) {
            this._scopes = new ArrayList();
        } else {
            this._scopes = new ArrayList(Arrays.asList(scopes));
        }

        if (allowedHosts != null && allowedHosts.length != 0) {
            this._hostValidator = new AllowedHostsValidator(allowedHosts);
        } else {
            this._hostValidator = new AllowedHostsValidator(new String[0]);
        }
    }

    @Nonnull
    public String getAuthorizationToken(@Nonnull final URI uri, @Nullable final Map<String, Object> additionalAuthenticationContext) {
        Span span;
        if (additionalAuthenticationContext != null && additionalAuthenticationContext.containsKey("parent-span") && additionalAuthenticationContext.get("parent-span") instanceof Span) {
            Span parentSpan = (Span)additionalAuthenticationContext.get("parent-span");
            span = GlobalOpenTelemetry.getTracer(tracerInstrumentationName).spanBuilder("getAuthorizationToken").setParent(Context.current().with(parentSpan)).startSpan();
        } else {
            span = GlobalOpenTelemetry.getTracer(tracerInstrumentationName).spanBuilder("getAuthorizationToken").startSpan();
        }

        String decodedClaim;
        try {
            Scope scope = span.makeCurrent();

            label223: {
                String var8;
                try {
                    if (!this._hostValidator.isUrlHostValid(uri)) {
                        span.setAttribute("com.microsoft.kiota.authentication.is_url_valid", false);
                        decodedClaim = "";
                        break label223;
                    }

                    if (!uri.getScheme().equalsIgnoreCase("https") && !isLocalhostUrl(uri.getHost())) {
                        span.setAttribute("com.microsoft.kiota.authentication.is_url_valid", false);
                        throw new IllegalArgumentException("Only https is supported");
                    }

                    span.setAttribute("com.microsoft.kiota.authentication.is_url_valid", true);
                    decodedClaim = null;
                    if (additionalAuthenticationContext != null && additionalAuthenticationContext.containsKey("claims") && additionalAuthenticationContext.get("claims") instanceof String) {
                        String rawClaim = (String)additionalAuthenticationContext.get("claims");

                        try {
                            decodedClaim = new String(Base64.getDecoder().decode(rawClaim), "UTF-8");
                        } catch (UnsupportedEncodingException var16) {
                            span.recordException(var16);
                        }
                    }

                    span.setAttribute("com.microsoft.kiota.authentication.additional_claims_provided", decodedClaim != null && !decodedClaim.isEmpty());
                    ArrayList scopes;
                    if (!this._scopes.isEmpty()) {
                        scopes = new ArrayList(this._scopes);
                    } else {
                        scopes = new ArrayList();
                        scopes.add(uri.getScheme() + "://" + uri.getHost() + "/.default");
                    }

                    TokenRequestContext context = new TokenRequestContext();
                    context.setScopes(scopes);
                    span.setAttribute("com.microsoft.kiota.authentication.scopes", String.join("|", scopes));
                    if (decodedClaim != null && !decodedClaim.isEmpty()) {
                        context.setClaims(decodedClaim);
                    }

                    var8 = this.creds.getTokenSync(context).getToken();
                } catch (Throwable var17) {
                    if (scope != null) {
                        try {
                            scope.close();
                        } catch (Throwable var15) {
                            var17.addSuppressed(var15);
                        }
                    }

                    throw var17;
                }

                if (scope != null) {
                    scope.close();
                }

                return var8;
            }

            if (scope != null) {
                scope.close();
            }
        } catch (IllegalArgumentException var18) {
            span.recordException(var18);
            throw var18;
        } finally {
            span.end();
        }

        return decodedClaim;
    }

    @Nonnull
    public AllowedHostsValidator getAllowedHostsValidator() {
        return this._hostValidator;
    }

    private static boolean isLocalhostUrl(@Nonnull String host) {
        Objects.requireNonNull(host);
        return localhostStrings.contains(host.toLowerCase(Locale.ROOT));
    }
}

