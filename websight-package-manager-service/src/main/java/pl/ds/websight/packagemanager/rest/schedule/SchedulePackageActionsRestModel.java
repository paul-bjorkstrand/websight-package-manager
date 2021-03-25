package pl.ds.websight.packagemanager.rest.schedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.rest.PackagePathValidatable;
import pl.ds.websight.request.parameters.support.annotations.RequestParameter;
import pl.ds.websight.rest.framework.Errors;

import javax.annotation.PostConstruct;
import javax.jcr.Session;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static pl.ds.websight.packagemanager.rest.Messages.PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH;

@Model(adaptables = SlingHttpServletRequest.class)
public class SchedulePackageActionsRestModel extends PackagePathValidatable {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulePackageActionsRestModel.class);

    private static final String SCHEDULE_ACTIONS_JOBS_PARAM_NAME = "actions";

    private static final CollectionType LIST_SCHEDULE_ACTION_COLLECTION_TYPE = TypeFactory.defaultInstance()
            .constructCollectionType(List.class, ScheduleAction.class);

    private static final ObjectReader SCHEDULE_ACTIONS_READER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
            .readerFor(LIST_SCHEDULE_ACTION_COLLECTION_TYPE);

    @SlingObject
    private ResourceResolver resourceResolver;

    @RequestParameter
    @NotBlank(message = PACKAGE_PATH_VALIDATION_ERROR_BLANK_PATH)
    private String path;

    @RequestParameter(name = SCHEDULE_ACTIONS_JOBS_PARAM_NAME)
    @NotBlank(message = "Actions to schedule cannot be blank")
    private String actionsJson;

    private List<ScheduleAction> allScheduleActions;
    private List<ScheduleAction> newActionsToSchedule;
    private List<ScheduleAction> scheduledActionsToUpdate;

    @PostConstruct
    private void init() {
        this.allScheduleActions = StringUtils.isNotBlank(actionsJson) ? readJson(actionsJson) : Collections.emptyList();
        this.newActionsToSchedule = new ArrayList<>();
        this.scheduledActionsToUpdate = new ArrayList<>();
        for (ScheduleAction scheduleAction : allScheduleActions) {
            if (StringUtils.isNotBlank(scheduleAction.getScheduleId())) {
                scheduledActionsToUpdate.add(scheduleAction);
            } else {
                newActionsToSchedule.add(scheduleAction);
            }
        }
    }

    private static List<ScheduleAction> readJson(String json) {
        try {
            return SCHEDULE_ACTIONS_READER.readValue(json);
        } catch (IOException e) {
            LOG.warn("Could not read jobs to schedule parameter", e);
        }
        return Collections.emptyList();
    }

    @Override
    protected String getPath() {
        return path;
    }

    public List<ScheduleAction> getNewActionsToSchedule() {
        return newActionsToSchedule;
    }

    public List<ScheduleAction> getScheduledActionsToUpdate() {
        return scheduledActionsToUpdate;
    }

    public Session getSession() {
        return resourceResolver.adaptTo(Session.class);
    }

    @Override
    public Errors validate() {
        Errors errors = super.validate();
        if (hasUnknownScheduleActionType()) {
            errors.add(SCHEDULE_ACTIONS_JOBS_PARAM_NAME, actionsJson,
                    "Schedule actions have to contain only selected operations: " +  Arrays.asList(ScheduleActionType.values()).toString());
        }
        insertAllSchedulesErrors(errors);
        if (containsScheduleDuplicates()) {
            errors.add(SCHEDULE_ACTIONS_JOBS_PARAM_NAME, actionsJson, "Each schedule action cannot contain any schedules duplicates");
        }
        Set<String> duplicatedIds = getScheduleActionsDuplicatedIds();
        if (!duplicatedIds.isEmpty()) {
            errors.add(SCHEDULE_ACTIONS_JOBS_PARAM_NAME, duplicatedIds, "List contains duplicated schedule actions to update");
        }
        return errors;
    }

    private boolean hasUnknownScheduleActionType() {
        return allScheduleActions.stream()
                .map(ScheduleAction::getActionType)
                .anyMatch(Objects::isNull);
    }

    private void insertAllSchedulesErrors(Errors errors) {
        allScheduleActions.stream()
                .map(ScheduleAction::getSchedules)
                .flatMap(Collection::stream)
                .map(Schedule::getError)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(errorPair -> errors.add(SCHEDULE_ACTIONS_JOBS_PARAM_NAME, errorPair.getKey(), errorPair.getValue()));
    }

    private boolean containsScheduleDuplicates() {
        return newActionsToSchedule.stream()
                .map(ScheduleAction::getSchedules)
                .anyMatch(schedules -> schedules.stream().distinct().count() != schedules.size());
    }

    private Set<String> getScheduleActionsDuplicatedIds() {
        List<String> ids = scheduledActionsToUpdate.stream()
                .map(scheduledJobToUpdate -> scheduledJobToUpdate.scheduleId)
                .collect(toList());
        return ids.stream()
                .filter(id -> Collections.frequency(ids, id) > 1)
                .collect(toSet());
    }

    public static class ScheduleAction {

        @JsonProperty("id")
        private String scheduleId;

        @JsonProperty("action")
        private ScheduleActionType actionType;

        private boolean suspended;

        @JsonDeserialize(using = OrderedScheduleListDeserializer.class)
        private List<Schedule> schedules;

        public String getScheduleId() {
            return scheduleId;
        }

        public ScheduleActionType getActionType() {
            return actionType;
        }

        public boolean isSuspended() {
            return suspended;
        }

        public List<Schedule> getSchedules() {
            return schedules;
        }
    }
}
