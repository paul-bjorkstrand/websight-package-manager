package pl.ds.websight.packagemanager.util;

public class OpenPackageException extends Exception {

    private static final String SIMPLIFIED_MESSAGE = "Provided package is not valid";

    private final String packagePath;

    public OpenPackageException(String packagePath) {
        this.packagePath = packagePath;
    }

    public String getSimplifiedMessage() {
        return SIMPLIFIED_MESSAGE;
    }

    @Override
    public String getMessage() {
        return "Provided path: " + packagePath + " does not point to a valid package";
    }
}
