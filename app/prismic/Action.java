package prismic;

import play.mvc.*;
import java.lang.annotation.*;

@With(prismic.ActionImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {}
