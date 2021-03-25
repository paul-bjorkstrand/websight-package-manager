package pl.ds.websight.packagemanager.rest.schedule;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class OrderedScheduleListDeserializer extends StdDeserializer<List<Schedule>> {

    private static final TypeReference<List<Schedule>> SCHEDULE_REF = new TypeReference<List<Schedule>>() {
    };

    private static final long serialVersionUID = 2632544173206427637L;

    public OrderedScheduleListDeserializer() {
        this(null);
    }

    protected OrderedScheduleListDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<Schedule> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return getSchedules(jsonParser).stream()
                .sorted()
                .collect(toList());
    }

    private static List<Schedule> getSchedules(JsonParser jsonParser) throws IOException {
        return jsonParser.readValueAs(SCHEDULE_REF);
    }
}
