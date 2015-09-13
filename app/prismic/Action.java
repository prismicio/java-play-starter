package prismic;

import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The @prismic.Action annotation creates a Prismic Context and stores it into the request.
 *
 * {@link Context}
 */
@With(ActionImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {}
