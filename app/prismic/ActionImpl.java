package prismic;

import io.prismic.Api;
import io.prismic.Cache;
import io.prismic.Ref;
import org.apache.commons.lang3.StringUtils;
import play.Configuration;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;

import static io.prismic.Prismic.EXPERIMENTS_COOKIE;
import static io.prismic.Prismic.PREVIEW_COOKIE;
import static prismic.Prismic.PRISMIC_CONTEXT;

public class ActionImpl extends play.mvc.Action<Action> {

    @Inject
    private Configuration configuration;

    // -- Define the key name to use for storing the Prismic.io access token into the Play session
    public final static String ACCESS_TOKEN = "ACCESS_TOKEN";

    // -- Cache to use (default to keep 200 JSON responses in a LRU cache)
    private final static Cache CACHE = new Cache.BuiltInCache(200);

    // -- Write debug and error messages to the Play `prismic` logger (check the configuration in application.conf)
    private final static Logger LOGGER = new Logger();

    /**
     * Enrich the Play session (a.k.a. HTTP Context) with a Prismic Context containing
     * <ul>
     * <li>an instance of Api</li>
     * <li>the Ref to be used (i.e. Master, a Release one, Preview or Experiment)</li>
     * <li>an access token (for private or restricted repository)</li>
     * <li>an instance of LinkResolver</li>
     * </ul>
     * before calling the annotated method.
     */
    public F.Promise<Result> call(Http.Context ctx) throws Throwable {

        String accessToken = retrieveAccessTokenFromPlaySessionOrConfiguration(ctx);

        Api api = getPrismicApi(accessToken);

        String ref = getPrismicRefFromPreviewOrExperimentCookieOrMaster(ctx, api);

        Context prismicContext = createPrismicContext(ctx, accessToken, api, ref);

        storePrismicContextIntoPlaySessionForFutureUse(ctx, prismicContext);

        return callAnnotatedMethodWithPrismicContext(ctx);
    }

    private String retrieveAccessTokenFromPlaySessionOrConfiguration(Http.Context ctx) {
        String accessToken = ctx.session().get(ACCESS_TOKEN);
        if (accessToken == null) {
            accessToken = configuration.getString("prismic.token");
        }
        return accessToken;
    }

    private Api getPrismicApi(String accessToken) {
        String key = "prismic.api";
        String apiEndpoint = configuration.getString(key);
        if (apiEndpoint == null) {
            throw new RuntimeException("Missing configuration [" + key + "]");
        }
        return Api.get(apiEndpoint, accessToken, CACHE, LOGGER);
    }

    private String getPrismicRefFromPreviewOrExperimentCookieOrMaster(Http.Context ctx, Api api) {
        // By default, use Master ref
        String ref = api.getMaster().getRef();

        String refLabel = configuration.getString("prismic.ref");
        if (StringUtils.isNotEmpty(refLabel)) {
            Ref apiRef = api.getRef(refLabel);
            if (apiRef == null) {
                throw new RuntimeException("Ref label [" + refLabel + "] not found or visibility restricted (in that " +
                        "case, change the API visibility or specify 'prismic.token' in your application.conf file)");
            } else {
                ref = apiRef.getRef();
            }
        }

        // In case of Preview request, use corresponding ref
        Http.Cookie previewCookie = ctx.request().cookie(PREVIEW_COOKIE);
        if (previewCookie != null) {
            ref = previewCookie.value();
        }

        // In case of Experiment request, use corresponding ref
        Http.Cookie experimentCookie = ctx.request().cookie(EXPERIMENTS_COOKIE);
        if (experimentCookie != null) {
            ref = api.getExperiments().refFromCookie(experimentCookie.value());
        }

        return ref;
    }

    private Context createPrismicContext(Http.Context ctx, String accessToken, Api api, String ref) {
        return new Context(api, ref, accessToken, new LinkResolver(api, ctx.request()));
    }

    private void storePrismicContextIntoPlaySessionForFutureUse(Http.Context ctx, Context prismicContext) {
        ctx.args.put(PRISMIC_CONTEXT, prismicContext);
    }

    private F.Promise<Result> callAnnotatedMethodWithPrismicContext(Http.Context ctx) throws Throwable {
        try {
            return delegate.call(ctx);
        } catch (Exception e) {
            if ("1".equals(Controller.flash("clearing"))) {
                // Prevent infinite redirect loop if the exception is not due to the preview cookie
                return delegate.call(ctx);
            } else {
                Controller.response().discardCookie(io.prismic.Prismic.PREVIEW_COOKIE);
                Controller.flash("clearing", "1");
                return F.Promise.pure(redirect(controllers.routes.Application.index()));
            }
        }
    }

}