package prismic;

import io.prismic.Api;
import io.prismic.Fragment;
import io.prismic.SimpleLinkResolver;
import play.mvc.Http;

public class LinkResolver extends SimpleLinkResolver {

    final Api api;
    final Http.Request request;

    public LinkResolver(Api api, Http.Request request) {
        this.api = api;
        this.request = request;
    }

    public String resolve(Fragment.DocumentLink link) {
        return controllers.routes.Application.detail(link.getId(), link.getSlug()).absoluteURL(request);
    }
}
