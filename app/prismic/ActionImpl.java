package prismic;

import javax.inject.Inject;

import play.mvc.*;
import play.Configuration;
import play.libs.F;

import io.prismic.Api;
import io.prismic.Cache;

import static io.prismic.Prismic.EXPERIMENTS_COOKIE;
import static io.prismic.Prismic.PREVIEW_COOKIE;
import static prismic.Prismic.ACCESS_TOKEN;
import static prismic.Prismic.PRISMIC_CONTEXT;

public class ActionImpl extends play.mvc.Action<prismic.Action> {

  @Inject
  private Configuration configuration;

  public F.Promise<Result> call(Http.Context ctx) throws Throwable {
    // Retrieve the accessToken from the Play session or from the configuration
    String accessToken = ctx.session().get(ACCESS_TOKEN);
    if(accessToken == null) {
      accessToken = configuration.getString("prismic.token");
    }

    // Retrieve the API
    Api api = prismic.Prismic.getApiHome(accessToken);

    // Use the ref from the preview cookie, experiment cookie or master
    String ref = api.getMaster().getRef();
    Http.Cookie previewCookie = ctx.request().cookie(io.prismic.Prismic.PREVIEW_COOKIE);
    Http.Cookie experimentCookie = ctx.request().cookie(io.prismic.Prismic.EXPERIMENTS_COOKIE);
    if (previewCookie != null) {
      ref = previewCookie.value();
    } else if (experimentCookie != null) {
      ref = api.getExperiments().refFromCookie(experimentCookie.value());
    }

    // Create the Prismic context
    Prismic.Context prismicContext = new Prismic.Context(api, ref, accessToken, controllers.Application.linkResolver(api, ctx.request()));

    // Strore it for future use
    ctx.args.put(PRISMIC_CONTEXT, prismicContext);

    // Go!
    try {
      return delegate.call(ctx);
    } catch (Exception e) {
      if ("1".equals(ctx.flash().get("clearing"))) {
        // Prevent infinite redirect loop if the exception is not due to the preview cookie
        return delegate.call(ctx);
      } else {
        ctx.response().discardCookie(io.prismic.Prismic.PREVIEW_COOKIE);
        ctx.flash().put("clearing", "1");
        return F.Promise.pure(redirect(controllers.routes.Application.index()));
      }
    }
  }

}
