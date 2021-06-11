/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils.queryparameters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public interface FOPParameters extends HasUrlParameter<String> {

    final Logger logger = (Logger) LoggerFactory.getLogger(FOPParameters.class);

    public default HashMap<String, List<String>> readParams(Location location,
            Map<String, List<String>> parametersMap) {

        HashMap<String, List<String>> newParameterMap = new HashMap<>(parametersMap);

        // get the fop from the query parameters, set to the default FOP if not provided
        FieldOfPlay fop = null;
        
        List<String> fopNames = parametersMap.get("fop");
        boolean fopFound = fopNames != null && fopNames.get(0) != null;
        if (!fopFound) {
            setShowInitialDialog(true);
        }
        
        if (!isIgnoreFopFromURL()) {
            if (fopFound) {
                logger.trace("fopNames {}", fopNames);
                fop = OwlcmsFactory.getFOPByName(fopNames.get(0));
            } else if (OwlcmsSession.getFop() != null) {
                logger.trace("OwlcmsSession.getFop() {}", OwlcmsSession.getFop());
                fop = OwlcmsSession.getFop();
            }
            if (fop == null) {
                logger.trace("OwlcmsFactory.getDefaultFOP() {}", OwlcmsFactory.getDefaultFOP());
                fop = OwlcmsFactory.getDefaultFOP();
            }
            newParameterMap.put("fop", Arrays.asList(URLUtils.urlEncode(fop.getName())));
            OwlcmsSession.setFop(fop);
        } else {
            newParameterMap.remove("fop");
        }

        // get the group from query parameters
        Group group = null;
        if (!isIgnoreGroupFromURL()) {
            List<String> groupNames = parametersMap.get("group");
            if (groupNames != null && groupNames.get(0) != null) {
                group = GroupRepository.findByName(groupNames.get(0));
                fop.loadGroup(group, this, true);
            } else {
                group = (fop != null ? fop.getGroup() : null);
            }
            if (group != null) {
                newParameterMap.put("group", Arrays.asList(URLUtils.urlEncode(group.getName())));
            }
        } else {
            newParameterMap.remove("group");
        }

        logger.debug("URL parsing: {} OwlcmsSession: fop={} group={}", LoggerUtils.whereFrom(),
                (fop != null ? fop.getName() : null), (group != null ? group.getName() : null));
        return newParameterMap;
    }

    public default void setShowInitialDialog(boolean b) {}
    public default boolean isShowInitialDialog() {
        return false;
    }

    public Location getLocation();

    public UI getLocationUI();

    public default boolean isIgnoreFopFromURL() {
        return false;
    }

    public default boolean isIgnoreGroupFromURL() {
        return true;
    }

    public void setLocation(Location location);

    public void setLocationUI(UI locationUI);

    /*
     * Retrieve parameter(s) from URL and update according to current settings.
     * 
     * The values are stored in the URL in order to allow bookmarking and easy reloading.
     * 
     * Note: what Vaadin calls a parameter is in the REST style, actually part of the URL path.
     * We use the old-style Query parameters for our purposes.
     *
     * @see com.vaadin.flow.router.HasUrlParameter#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.Object)
     */
    @Override
    public default void setParameter(BeforeEvent event, @OptionalParameter String unused) {
        logger.setLevel(Level.INFO);
        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        HashMap<String, List<String>> params = readParams(location, parametersMap);
        
        // change the URL to reflect the updated parameters
        event.getUI().getPage().getHistory().replaceState(null,
                new Location(location.getPath(), new QueryParameters(params)));
    }
    
    public default void updateParam(Map<String, List<String>> cleanParams, String parameter, String value) {
        if (value != null) {
            cleanParams.put(parameter, Arrays.asList(value));
        } else {
            cleanParams.remove(parameter);
        }
    }

    public default void updateURLLocation(UI ui, Location location, String parameter, String mode) {
        TreeMap<String, List<String>> parametersMap = new TreeMap<>(location.getQueryParameters().getParameters());
        // get current values
        FieldOfPlay fop = OwlcmsSession.getFop();
        updateParam(parametersMap, "fop", fop != null ? fop.getName() : null);
        
        // override with the update
        updateParam(parametersMap, parameter, mode);
        
        Location location2 = new Location(location.getPath(), new QueryParameters(parametersMap));
        ui.getPage().getHistory().replaceState(null, location2);
        setLocation(location2);
    }


}