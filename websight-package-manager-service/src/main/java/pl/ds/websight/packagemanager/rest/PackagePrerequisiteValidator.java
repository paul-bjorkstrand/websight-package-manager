package pl.ds.websight.packagemanager.rest;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.vault.packaging.JcrPackage;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class PackagePrerequisiteValidator {

    private final Predicate<JcrPackage> prerequisite;
    private final String simpleMessage;
    private final UnaryOperator<String> extendedMessageGenerator;

    public PackagePrerequisiteValidator(Predicate<JcrPackage> prerequisite, String simpleMessage,
            UnaryOperator<String> extendedMessageGenerator) {
        this.prerequisite = prerequisite;
        this.simpleMessage = simpleMessage;
        this.extendedMessageGenerator = extendedMessageGenerator;
    }

    public boolean failPrerequisite(JcrPackage jcrPackage) {
        return !prerequisite.test(jcrPackage);
    }

    public String getSimpleMessage() {
        return simpleMessage;
    }

    public String getExtendedMessage(String packagePath) {
        return extendedMessageGenerator.apply(packagePath);
    }

    public static Pair<String, String> getValidationResult(PackagePrerequisiteValidator[] validators, JcrPackage packageToValidate,
            String packagePath) {
        if (validators != null) {
            for (PackagePrerequisiteValidator validator : validators) {
                if (validator.failPrerequisite(packageToValidate)) {
                    return ImmutablePair.of(validator.getSimpleMessage(), validator.getExtendedMessage(packagePath));
                }
            }
        }
        return ImmutablePair.nullPair();
    }
}
