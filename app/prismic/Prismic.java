package prismic;

import play.mvc.Http;

/**
 * Required to be called in a Play session enriched with a Prismic Context thanks to @prismic.Action annotation.
 */
public class Prismic extends io.prismic.Prismic {

    // -- Define the key name to use for storing the Prismic context token into the request arguments
    public final static String PRISMIC_CONTEXT = "PRISMIC_CONTEXT";

    // -- Retrieve the Prismic Context from a request handled by an built using Prismic.action
    public Context getContext() {
        Context ctx = (Context) Http.Context.current().args.get(PRISMIC_CONTEXT);
        if (ctx == null) {
            throw new RuntimeException("No Context API found - Is it a @prismic.Action?");
        }
        return ctx;
    }


}
