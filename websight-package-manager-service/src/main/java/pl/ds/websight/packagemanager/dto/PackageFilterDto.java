package pl.ds.websight.packagemanager.dto;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.ds.websight.packagemanager.util.JcrPackageUtil;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.apache.jackrabbit.vault.packaging.JcrPackageDefinition.PN_MODE;
import static org.apache.jackrabbit.vault.packaging.JcrPackageDefinition.PN_ROOT;
import static org.apache.jackrabbit.vault.packaging.JcrPackageDefinition.PN_RULES;

public class PackageFilterDto {

    private static final Logger LOG = LoggerFactory.getLogger(PackageFilterDto.class);

    private final String root;

    private final ImportMode mode;

    private final List<PackageFilterRuleDto> rules;

    private PackageFilterDto(String root, ImportMode mode, List<PackageFilterRuleDto> rules) {
        this.root = root;
        this.mode = mode;
        this.rules = rules;
    }

    public static PackageFilterDto create(Node filterNode) {
        try {
            return new PackageFilterDto(filterNode.getProperty(PN_ROOT).getString(), getImportMode(filterNode), getFilterRules(filterNode));
        } catch (RepositoryException e) {
            LOG.warn("Could not fetch Package Filters from node properties", e);
        }
        return null;
    }

    private static ImportMode getImportMode(Node filterNode) throws RepositoryException {
        Property modeProp = filterNode.getProperty(PN_MODE);
        return modeProp != null ?
                JcrPackageUtil.toImportMode(modeProp.getString()) :
                ImportMode.REPLACE;
    }

    private static List<PackageFilterRuleDto> getFilterRules(Node filterNode) throws RepositoryException {
        Property rulesProp = filterNode.getProperty(PN_RULES);
        Value[] ruleValues = rulesProp.isMultiple() ?
                rulesProp.getValues() :
                new Value[]{rulesProp.getValue()};

        return Arrays.stream(ruleValues)
                .map(PackageFilterRuleDto::create)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public String getRoot() {
        return root;
    }

    public ImportMode getMode() {
        return mode;
    }

    public List<PackageFilterRuleDto> getRules() {
        return rules;
    }

    private static class PackageFilterRuleDto {

        private static final String MODIFIER_PATTERN_SEPARATOR = ":";

        private final boolean include;

        private final String pattern;

        private PackageFilterRuleDto(String rule) {
            this.include = "include".equals(extractIncludeInfo(rule));
            this.pattern = extractPattern(rule);
        }

        private static PackageFilterRuleDto create(Value ruleValue) {
            String rule = getRuleText(ruleValue);
            if (isValid(rule)) {
                return new PackageFilterRuleDto(rule);
            }
            LOG.warn("Rule: {} is malformed and will be skipped during processing", rule);
            return null;
        }

        private static String getRuleText(Value ruleValue) {
            try {
                return ruleValue.getString();
            } catch (RepositoryException e) {
                LOG.warn("Could not get Filter rule as text", e);
            }
            return null;
        }

        private static boolean isValid(String rule) {
            return StringUtils.contains(rule, MODIFIER_PATTERN_SEPARATOR) &&
                    extractPattern(rule).length() > 0 &&
                    StringUtils.containsAny(extractIncludeInfo(rule), "include", "exclude");
        }

        private static String extractPattern(String rule) {
            return StringUtils.substringAfter(rule, MODIFIER_PATTERN_SEPARATOR);
        }

        private static String extractIncludeInfo(String rule) {
            return StringUtils.substringBefore(rule, MODIFIER_PATTERN_SEPARATOR);
        }

        public boolean isInclude() {
            return include;
        }

        public String getPattern() {
            return pattern;
        }
    }
}