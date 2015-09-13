package prismic;

import io.prismic.Api;

/**
 * A Prismic Context that helps to keep the reference to useful primisc.io contextual data
 */
public class Context {

    final Api api;
    final String ref;
    final String accessToken;
    final LinkResolver linkResolver;

    public Context(Api api, String ref, String accessToken, LinkResolver linkResolver) {
        this.api = api;
        this.ref = ref;
        this.accessToken = accessToken;
        this.linkResolver = linkResolver;
    }

    public Api getApi() {
        return api;
    }

    public String getRef() {
        return ref;
    }

    public LinkResolver getLinkResolver() {
        return linkResolver;
    }

}