package prismic;

public class Logger implements io.prismic.Logger {
    public void log(String level, String message) {
        play.Logger.ALogger logger = play.Logger.of("prismic");
        if ("DEBUG".equals(level)) {
            logger.debug(message);
        } else if ("ERROR".equals(level)) {
            logger.error(message);
        } else if ("WARNING".equals(level)) {
            logger.warn(message);
        } else {
            logger.info(message);
        }
    }
}
