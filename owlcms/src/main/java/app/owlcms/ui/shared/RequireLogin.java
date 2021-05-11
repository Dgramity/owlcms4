/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.shared;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.home.LoginView;
import app.owlcms.utils.AccessUtils;
import ch.qos.logback.classic.Logger;

public interface RequireLogin extends BeforeEnterObserver {
    
    Logger logger = (Logger) LoggerFactory.getLogger(RequireLogin.class);

    @Override
    public default void beforeEnter(BeforeEnterEvent event) {
        OwlcmsFactory.waitDBInitialized();
        boolean isAuthenticated = OwlcmsSession.isAuthenticated();
        if (isAuthenticated) {
            // no check required
            OwlcmsSession.setAuthenticated(true);
            return;
        }

        String path = event.getLocation().getPath();
        String whiteList = Config.getCurrent().getParamAccessList();
        String pin = Config.getCurrent().getParamPin();
        String backdoorList = Config.getCurrent().getParamBackdoorList();

        boolean noPin = pin == null || pin.isBlank();
        boolean noWhiteList = whiteList == null || whiteList.isBlank();
        boolean backdoor = backdoorList != null && !backdoorList.isBlank();
        if ((noPin && noWhiteList)) {
            // no check required
            OwlcmsSession.setAuthenticated(true);
            return;
        } else if (backdoor && AccessUtils.checkBackdoor()) {
            // explicit backdoor access allowed (e.g. for video capture of browser screens)
            logger.info("allowing backdoor access from {}",AccessUtils.getClientIp());
            OwlcmsSession.setAuthenticated(true);
            return;
        } else if (noPin && AccessUtils.checkWhitelist()) {
            // no pin required, proper origin, no need to challenge
            OwlcmsSession.setAuthenticated(true);
            return;
        } else if (!path.equals(LoginView.LOGIN)) {
            // prompt user for PIN
            // (if whitelist membership is required, will be prompted even if no PIN is
            // required, so that an error message is shown)
            OwlcmsSession.setRequestedUrl(path);
            event.forwardTo(LoginView.LOGIN);
        } else {
            // already on login view, do nothing.
            // login will send to home.
        }
    }

}
