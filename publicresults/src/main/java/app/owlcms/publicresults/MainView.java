/***
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.publicresults;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.displays.scoreboard.ScoreWithLeaders;
import app.owlcms.i18n.Translator;
import app.owlcms.uievents.UpdateEvent;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

@Route
@Push
public class MainView extends VerticalLayout {

    static Text text;

    private static Logger logger = (Logger) LoggerFactory.getLogger(MainView.class);
    private UI ui;

    public MainView() {
        logger.warn("mainView");
        text = new Text(Translator.translate("WaitingForSite"));
        ui = UI.getCurrent();
        if (ui != null) {
            buildHomePage();
        }
    }

    @Subscribe
    public void update(UpdateEvent e) {
        logger.warn("update");
        if (ui == null) {
            logger.error("ui is null!?");
            return;
        }
        ui.access(() -> {
            buildHomePage();
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logger.warn("onAttach");
        super.onAttach(attachEvent);
        ui = UI.getCurrent();
        UpdateReceiverServlet.getEventBus().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        UpdateReceiverServlet.getEventBus().unregister(this);
    }

    private void buildHomePage() {
        // we cache the last update received for each field of play, indexed by fop name
        Set<String> fopNames = UpdateReceiverServlet.updateCache.keySet();
        logger.warn("buildHomePage {} {}", fopNames.size(), ui);
        if (fopNames.size() == 0 || ui == null) {
            removeAll();
            add(text);
        } else if (fopNames.size() == 1) {
            logger.warn("single platform, go to scoreboard");
            Map<String, String> parameterMap = new HashMap<>();
            String fop = fopNames.stream().findFirst().get();
            parameterMap.put("FOP", fop);
            // ui.navigate("displays/scoreleader", QueryParameters.simple(parameterMap));
            ui.getPage().executeJs("window.location.href='displays/scoreleader?FOP=" + fop + "'");
        } else {
            createButtons(fopNames);
        }
    }

    private void createButtons(Set<String> fopNames) {
        removeAll();
        UpdateEvent updateEvent = UpdateReceiverServlet.updateCache.entrySet().stream().findFirst().orElse(null)
                .getValue();
        if (updateEvent == null) {
            return;
        }

        H3 title = new H3(updateEvent.getCompetitionName());
        add(title);
        fopNames.stream().sorted().forEach(fopName -> {
            Button fopButton = new Button(getTranslation("Platform") + " " + fopName,
                    buttonClickEvent -> {
                        String url = URLUtils.getRelativeURLFromTargetClass(ScoreWithLeaders.class);
                        HashMap<String, List<String>> params = new HashMap<>();
                        params.put("fop", Arrays.asList(fopName));
                        QueryParameters parameters = new QueryParameters(params);
                        UI.getCurrent().navigate(url, parameters);
                    });
            add(fopButton);
        });
    }

}
