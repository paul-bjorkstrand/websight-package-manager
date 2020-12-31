package pl.ds.websight.packagemanager.auth;

import org.osgi.service.component.annotations.Component;
import pl.ds.websight.admin.auth.AnonymousAccessEnabler;

@Component(service = AnonymousAccessEnabler.class)
public class PackageManagerAnonymousAccessEnabler implements AnonymousAccessEnabler {

    @Override
    public String[] getPaths() {
        return new String[] { "/apps/websight-package-manager-service" };
    }
}