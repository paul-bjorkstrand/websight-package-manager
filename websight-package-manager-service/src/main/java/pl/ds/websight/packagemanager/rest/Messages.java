package pl.ds.websight.packagemanager.rest;

import static pl.ds.websight.packagemanager.util.JcrPackageUtil.PACKAGES_ROOT_PATH;

public class Messages {

    // Create package:
    public static final String CREATE_PACKAGE_SUCCESS = "Package created";
    public static final String CREATE_PACKAGE_SUCCESS_DETAILS = "Package '%s' has been created";
    public static final String CREATE_PACKAGE_ERROR = "Could not create package";
    public static final String CREATE_PACKAGE_ERROR_CREATED_PARTIALLY = "Package created partially";
    public static final String CREATE_PACKAGE_ERROR_CREATED_PARTIALLY_DETAILS = "Package '%s' has been created, but could not save details";
    public static final String CREATE_PACKAGE_ERROR_ALREADY_EXISTS = "Package already exists";
    public static final String CREATE_PACKAGE_ERROR_ALREADY_EXISTS_DETAILS = "Package '%s' already exists";

    // Delete package:
    public static final String DELETE_PACKAGE_SUCCESS = "Package deleted";
    public static final String DELETE_PACKAGE_SUCCESS_DETAILS = "Package '%s' has been deleted";
    public static final String DELETE_PACKAGE_ERROR = "Could not delete package";

    // Edit package:
    public static final String EDIT_PACKAGE_SUCCESS = "Package edited";
    public static final String EDIT_PACKAGE_SUCCESS_DETAILS = "Package '%s' has been modified";
    public static final String EDIT_PACKAGE_ERROR = "Could not edit package";
    public static final String EDIT_PACKAGE_ERROR_ACCESS_DENIED_DETAILS = "You are not allowed to edit package '%s'";
    public static final String EDIT_PACKAGE_ERROR_CAN_NOT_RENAME_DETAILS = "Could not rename package '%s'";
    public static final String EDIT_PACKAGE_ERROR_DETAILS = "An error occurred while editing package '%s'";
    public static final String EDIT_PACKAGE_ERROR_CAN_NOT_UPDATE_SCHEDULED_ACTIONS_DETAILS =
            "Could not change action type in existing schedule for package '%s'";

    // Find packages:
    public static final String FIND_PACKAGES_ERROR = "Could not fetch packages";

    // Get groups:
    public static final String GET_GROUPS_ERROR = "Could not fetch groups";

    // Get package action report:
    public static final String GET_PACKAGE_ACTION_REPORT_ERROR = "Could not get or access package action logs";
    public static final String GET_PACKAGE_ACTION_REPORT_ERROR_DETAILS = "An error occurred while fetching logs";

    // Get package action full log:
    public static final String GET_PACKAGE_LOG_ERROR_NO_USER_SESSION = "Could not access user's session";
    public static final String GET_PACKAGE_LOG_ERROR_NO_LOGS_DETAILS = "Could not get or access package action logs for package '%s'";

    // Get package actions:
    public static final String GET_PACKAGE_ACTION_ERROR = "Could not get package action state";

    // Action package:
    public static final String PACKAGE_ACTION_SUCCESS = "%s queued";
    public static final String PACKAGE_ACTION_SUCCESS_DETAILS = "%s queued for package '%s'";
    public static final String PACKAGE_ACTION_ERROR = "Could not %s package";
    public static final String PACKAGE_ACTION_ERROR_NOT_QUEUED_DETAILS = "Could not add %s to job queue";
    public static final String PACKAGE_ACTION_ERROR_ALREADY_USED_DETAILS = "%s of package '%s' is already in progress";

    // Schedule package actions:
    public static final String SCHEDULE_PACKAGE_ACTIONS_SUCCESS = "Actions scheduled";
    public static final String SCHEDULE_PACKAGE_ACTIONS_SUCCESS_DETAILS = "Actions scheduled for package '%s'";
    public static final String SCHEDULE_PACKAGE_ACTIONS_ERROR = "Could not schedule actions";
    public static final String SCHEDULE_PACKAGE_ACTIONS_ERROR_CHANGED_ACTION_IN_SCHEDULED_JOB_DETAILS =
            "Could not change scheduled action type in existed schedule for package '%s'";
    public static final String SCHEDULE_PACKAGE_ACTIONS_ERROR_NOT_SCHEDULED_DETAILS = "Could not schedule %s for package '%s'";
    public static final String SCHEDULE_PACKAGE_ACTIONS_ERROR_ALREADY_SCHEDULED_DETAILS =
            "%s of package '%s' is already scheduled for this time";

    // Get scheduled package actions:
    public static final String GET_PACKAGE_SCHEDULED_ACTIONS_ERROR = "Could not get scheduled package actions";
    public static final String GET_PACKAGE_SCHEDULED_ACTIONS_ERROR_NO_PACKAGE_NODE_DETAILS = "Could not get package node for path '%s'";


    // Edit scheduled package actions:
    public static final String EDIT_SCHEDULED_PACKAGE_ACTIONS_ERROR_NOT_SCHEDULED_DETAILS =
            "Could not reschedule %s for package '%s' to scheduled jobs queue";
    public static final String EDIT_SCHEDULED_PACKAGE_ACTIONS_ERROR_NO_SCHEDULE_DETAILS =
            "Could not find schedule with id '%s' for package '%s'";

    // Cancel package action:
    public static final String CANCEL_PACKAGE_ACTION_ERROR = "Could not cancel package action";
    public static final String CANCEL_PACKAGE_ACTION_ERROR_NO_ACTIONS = "There are no queued actions";
    public static final String CANCEL_PACKAGE_ACTION_ERROR_NO_ACTIONS_DETAILS = "There are no queued actions for package '%s'";
    public static final String CANCEL_PACKAGE_ACTION_ERROR_UNSUCCESSFUL_DELETE = "Could not cancel queued %s";
    public static final String CANCEL_PACKAGE_ACTION_ERROR_UNSUCCESSFUL_DELETE_DETAILS = "Could not cancel queued %s for package '%s'";
    public static final String CANCEL_PACKAGE_ACTION_SUCCESS = "%s cancelled";
    public static final String CANCEL_PACKAGE_ACTION_SUCCESS_DETAILS = "Cancelled %s and other queued actions for package '%s'";

    // Upload package:
    public static final String UPLOAD_PACKAGE_SUCCESS = "Package uploaded";
    public static final String UPLOAD_PACKAGE_SUCCESS_DETAILS = "Package '%s' has been uploaded";
    public static final String UPLOAD_PACKAGE_ERROR = "Could not upload package";
    public static final String UPLOAD_PACKAGE_ERROR_ALREADY_EXISTS_DETAILS = "Uploaded package already exists";
    public static final String UPLOAD_PACKAGE_ERROR_CONTENT_TYPE_DETAILS = "Content Type of request is not '%s'";
    public static final String UPLOAD_PACKAGE_ERROR_FILE_IS_FORM_FIELD_DETAILS = "Parameter 'file' should be a file";

    // Package actions errors:
    public static final String BUILD_PACKAGE_ERROR = "Could not build package";
    public static final String COVERAGE_PACKAGE_ERROR = "Could not get package coverage";
    public static final String INSTALL_PACKAGE_ERROR = "Could not install package";
    public static final String UNINSTALL_PACKAGE_ERROR = "Could not uninstall package";

    // Package elements validation:
    public static final String PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH = "Path cannot be blank";
    public static final String PACKAGE_PATH_VALIDATION_ERROR_INVALID_PATH = "Path must start with '" + PACKAGES_ROOT_PATH + "'";
    public static final String PACKAGE_PATHS_VALIDATION_ERROR_INVALID_PATHS = "Selected paths must start with '" + PACKAGES_ROOT_PATH + "'";
    public static final String PACKAGE_ID_VALIDATION_ERROR_INVALID_COMBINATION = "Invalid combination of name, group and version";
    public static final String PACKAGE_NAME_VALIDATION_ERROR_PATH_ALREADY_EXISTS = "Package or group under path '%s' already exists";

    // Combined package actions:
    public static final String COMBINED_PACKAGE_ACTIONS_SUCCESS = "Package %s and %s queued";
    public static final String COMBINED_PACKAGE_ACTIONS_SUCCESS_DETAILS = "Package has been %s and %s is queued for package '%s'";
    public static final String COMBINED_PACKAGE_ACTIONS_ERROR_NO_SAVED_STATE_FIRST_ACTION = "Package %s, but could not queue %s";
    public static final String COMBINED_PACKAGE_ACTIONS_NO_SAVED_STATE_FIRST_ACTION_DETAILS =
            "Package %s, but the state was not saved, so '%s' could not be queued";
    public static final String COMBINED_PACKAGE_ACTIONS_ERROR_SECOND_ACTION = "Package %s, %s not queued";

    private Messages() {
        // no instances
    }

    public static String formatMessage(String message, Object... args) {
        return String.format(message, args);
    }
}
