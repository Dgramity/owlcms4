/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.components.DownloadDialog;
import app.owlcms.components.fields.LocalDateField;
import app.owlcms.components.fields.LocalizedDecimalField;
import app.owlcms.components.fields.ValidationTextField;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsComboBoxProvider;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.crudui.OwlcmsMultiSelectComboBoxProvider;
import app.owlcms.nui.shared.AthleteRegistrationFormFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCards;
import app.owlcms.spreadsheet.JXLSStartingList;
import app.owlcms.utils.NaturalOrderComparator;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class RegistrationContent
 *
 * Defines the toolbar and the table for editing registration data on athletes.
 *
 */
@SuppressWarnings("serial")
@Route(value = "npreparation/athletes", layout = OwlcmsLayout.class)
@CssImport(value = "./styles/shared-styles.css")
public class RegistrationContent extends VerticalLayout implements CrudListener<Athlete>, OwlcmsContent, FOPParameters {

    final static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
    private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
    private ComboBox<Category> categoryFilter = new ComboBox<>();
    private OwlcmsCrudGrid<Athlete> crudGrid;
    private Group currentGroup;
    private ComboBox<Gender> genderFilter = new ComboBox<>();

    private ComboBox<Group> groupFilter = new ComboBox<>();
    private TextField lastNameFilter = new TextField();
    private Location location;
    private UI locationUI;
    private OwlcmsLayout routerLayout;
    private ComboBox<Boolean> weighedInFilter = new ComboBox<>();

    /**
     * Instantiates the athlete crudGrid
     */
    public RegistrationContent() {
        logger.warn("::::: creating registration content");
        OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
        crudGrid = createGrid(crudFormFactory);
        defineFilters(crudGrid);
        fillHW(crudGrid, this);
    }
    
    @Override
    public void setHeaderContent() {
        routerLayout.setTopBarTitle(getPageTitle());
        routerLayout.setMenuArea(createMenuArea());
        routerLayout.showLocaleDropdown(false);
        routerLayout.setDrawerOpened(false);
        routerLayout.updateHeader();
    }

    @Override
    public Athlete add(Athlete athlete) {
        if (athlete.getGroup() == null && currentGroup != null) {
            athlete.setGroup(currentGroup);
        }
        ((OwlcmsCrudFormFactory<Athlete>) crudGrid.getCrudFormFactory()).add(athlete);
        return athlete;
    }

    @Override
    public void delete(Athlete athlete) {
        ((OwlcmsCrudFormFactory<Athlete>) crudGrid.getCrudFormFactory()).delete(athlete);
        return;
    }

    public Collection<Athlete> doFindAll(EntityManager em) {
        List<Athlete> all = AthleteRepository.doFindFiltered(em, lastNameFilter.getValue(), groupFilter.getValue(),
                categoryFilter.getValue(), ageGroupFilter.getValue(), ageDivisionFilter.getValue(),
                genderFilter.getValue(), weighedInFilter.getValue(), -1, -1);
        return all;
    }

    /**
     * The refresh button on the toolbar; also called by refreshGrid when the group is changed.
     *
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<Athlete> findAll() {
        List<Athlete> findFiltered = AthleteRepository.findFiltered(lastNameFilter.getValue(), groupFilter.getValue(),
                categoryFilter.getValue(), ageGroupFilter.getValue(), ageDivisionFilter.getValue(),
                genderFilter.getValue(), weighedInFilter.getValue(), -1, -1);
        AthleteSorter.registrationOrder(findFiltered);
        return findFiltered;
    }

    /**
     * @return the groupFilter
     */
    public ComboBox<Group> getGroupFilter() {
        return groupFilter;
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public UI getLocationUI() {
        return this.locationUI;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("EditRegisteredAthletes");
    }

    @Override
    public OwlcmsLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public boolean isIgnoreFopFromURL() {
        return true;
    }

    @Override
    public boolean isIgnoreGroupFromURL() {
        return false;
    }

    public void refresh() {
        crudGrid.refreshGrid();
    }

    public void refreshCrudGrid() {
        crudGrid.refreshGrid();
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    /**
     * Parse the http query parameters
     *
     * Note: because we have the @Route, the parameters are parsed *before* our parent layout is created.
     *
     * @param event     Vaadin navigation event
     * @param parameter null in this case -- we don't want a vaadin "/" parameter. This allows us to add query
     *                  parameters instead.
     *
     * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.String)
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        setLocation(event.getLocation());
        setLocationUI(event.getUI());
        QueryParameters queryParameters = getLocation().getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters(); // immutable
        HashMap<String, List<String>> params = new HashMap<>(parametersMap);

        //logger.trace("parsing query parameters RegistrationContent");
        List<String> groupNames = params.get("group");
        //logger.trace("groupNames = {}", groupNames);
        if (!isIgnoreGroupFromURL() && groupNames != null && !groupNames.isEmpty()) {
            String groupName = groupNames.get(0);
            groupName = URLDecoder.decode(groupName, StandardCharsets.UTF_8);
            currentGroup = GroupRepository.findByName(groupName);
        } else {
            currentGroup = null;
        }
        if (currentGroup != null) {
            params.put("group", Arrays.asList(URLUtils.urlEncode(currentGroup.getName())));
            OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
            crudGrid.setCrudFormFactory(crudFormFactory);
        } else {
            params.remove("group");
        }

        params.remove("fop");

        // change the URL to reflect group
        event.getUI().getPage().getHistory().replaceState(null,
                new Location(getLocation().getPath(), new QueryParameters(params)));
    }

    @Override
    public void setRouterLayout(OwlcmsLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Override
    public Athlete update(Athlete athlete) {
        OwlcmsSession.setAttribute("weighIn", athlete);
        Athlete a = ((OwlcmsCrudFormFactory<Athlete>) crudGrid.getCrudFormFactory()).update(athlete);
        OwlcmsSession.setAttribute("weighIn", null);
        return a;
    }

    /**
     * Define the form used to edit a given athlete.
     *
     * @return the form factory that will create the actual form on demand
     */
    protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
        OwlcmsCrudFormFactory<Athlete> athleteEditingFormFactory = new AthleteRegistrationFormFactory(Athlete.class, currentGroup);
        createFormLayout(athleteEditingFormFactory);
        return athleteEditingFormFactory;
    }

    /**
     * The columns of the crudGrid
     *
     * @param crudFormFactory what to call to create the form for editing an athlete
     * @return
     */
    protected OwlcmsCrudGrid<Athlete> createGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        Grid<Athlete> grid = new Grid<>(Athlete.class, false);
        grid.addColumn("lotNumber").setHeader(getTranslation("Lot"));
        grid.addColumn("lastName").setHeader(getTranslation("LastName"));
        grid.addColumn("firstName").setHeader(getTranslation("FirstName"));
        grid.addColumn("team").setHeader(getTranslation("Team"));
        grid.addColumn("yearOfBirth").setHeader(getTranslation("BirthDate"));
        grid.addColumn("gender").setHeader(getTranslation("Gender"));
        grid.addColumn("ageGroup").setHeader(getTranslation("AgeGroup"));
        grid.addColumn("category").setHeader(getTranslation("Category"));
        grid.addColumn(new NumberRenderer<>(Athlete::getBodyWeight, "%.2f", this.getLocale()), "bodyWeight")
                .setHeader(getTranslation("BodyWeight"));
        grid.addColumn("group").setHeader(getTranslation("Group"));
        grid.addColumn("eligibleCategories").setHeader(getTranslation("Registration.EligibleCategories"));
        grid.addColumn("entryTotal").setHeader(getTranslation("EntryTotal"));
        grid.addColumn("federationCodes").setHeader(getTranslation("Registration.FederationCodes"));
        OwlcmsCrudGrid<Athlete> crudGrid = new OwlcmsCrudGrid<>(Athlete.class, new OwlcmsGridLayout(Athlete.class) {
            @Override
            public void hideForm() {
                super.hideForm();
                logger.trace("clearing {}", OwlcmsSession.getAttribute("weighIn"));
                OwlcmsSession.setAttribute("weighIn", null);
            }
        },
                crudFormFactory, grid);
        crudGrid.setCrudListener(this);
        crudGrid.setClickRowToUpdate(true);
        return crudGrid;
    }

    /**
     * The filters at the top of the crudGrid
     *
     * @param crudGrid the crudGrid that will be filtered.
     */
    protected void defineFilters(OwlcmsCrudGrid<Athlete> crudGrid) {
        logger.warn("::::: defining filters");
        lastNameFilter.setPlaceholder(getTranslation("LastName"));
        lastNameFilter.setClearButtonVisible(true);
        lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        lastNameFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        lastNameFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(lastNameFilter);

        ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision"));
        ageDivisionFilter.setItems(AgeDivision.findAll());
        ageDivisionFilter.setItemLabelGenerator((ad) -> getTranslation("Division." + ad.name()));
        ageDivisionFilter.setClearButtonVisible(true);
        ageDivisionFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        ageDivisionFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(ageDivisionFilter);

        ageGroupFilter.setPlaceholder(getTranslation("AgeGroup"));
        ageGroupFilter.setItems(AgeGroupRepository.findAll());
        // ageGroupFilter.setItemLabelGenerator(AgeDivision::name);
        ageGroupFilter.setClearButtonVisible(true);
        ageGroupFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        ageGroupFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(ageGroupFilter);

        categoryFilter.setPlaceholder(getTranslation("Category"));
        categoryFilter.setItems(CategoryRepository.findActive());
        categoryFilter.setItemLabelGenerator(Category::getName);
        categoryFilter.setClearButtonVisible(true);
        categoryFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        categoryFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(categoryFilter);

        groupFilter.setPlaceholder(getTranslation("Group"));
        List<Group> groups = GroupRepository.findAll();
        groups.sort((Comparator<Group>) new NaturalOrderComparator<Group>());
        groupFilter.setItems(groups);
        groupFilter.setItemLabelGenerator(Group::getName);
        groupFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
            currentGroup = e.getValue();
            updateURLLocation(getLocationUI(), getLocation(), e.getValue());
        });
        groupFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(groupFilter);
        groupFilter.getStyle().set("display", "none");

        weighedInFilter.setPlaceholder(getTranslation("Weighed_in_p"));
        weighedInFilter.setItems(Boolean.TRUE, Boolean.FALSE);
        weighedInFilter.setItemLabelGenerator((i) -> {
            return i ? getTranslation("Weighed") : getTranslation("Not_weighed");
        });
        weighedInFilter.setClearButtonVisible(true);
        weighedInFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        weighedInFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(weighedInFilter);

        genderFilter.setPlaceholder(getTranslation("Gender"));
        genderFilter.setItems(Gender.M, Gender.F);
        genderFilter.setItemLabelGenerator((i) -> {
            return i == Gender.M ? getTranslation("Gender.M") : getTranslation("Gender.F");
        });
        genderFilter.setClearButtonVisible(true);
        genderFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        genderFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(genderFilter);

        Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
        clearFilters.addClickListener(event -> {
            lastNameFilter.clear();
            ageDivisionFilter.clear();
            categoryFilter.clear();
            groupFilter.clear();
            weighedInFilter.clear();
            genderFilter.clear();
        });
        lastNameFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(clearFilters);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getRouterLayout().closeDrawer();
        getGroupSelect().setValue(currentGroup);
    }

    /**
     * The content and ordering of the editing form
     *
     * @param crudFormFactory the factory that will create the form using this information
     */
    private void createFormLayout(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        List<String> props = new LinkedList<>();
        List<String> captions = new LinkedList<>();

        props.add("lastName");
        captions.add(getTranslation("LastName"));
        props.add("firstName");
        captions.add(getTranslation("FirstName"));
        props.add("gender");
        captions.add(getTranslation("Gender"));
        props.add("team");
        captions.add(getTranslation("Team"));

        Competition competition = Competition.getCurrent();
        if (competition.isUseBirthYear()) {
            props.add("yearOfBirth");
            captions.add(getTranslation("YearOfBirth"));
        } else {
            props.add("fullBirthDate");
            captions.add(getTranslation("BirthDate_yyyy"));
        }
        props.add("group");
        captions.add(getTranslation("Group"));

        props.add("bodyWeight");
        captions.add(getTranslation("BodyWeight"));
        props.add("snatch1Declaration");
        captions.add(getTranslation("SnatchDecl_"));
        props.add("cleanJerk1Declaration");
        captions.add(getTranslation("C_and_J_decl"));
        props.add("qualifyingTotal");
        captions.add(getTranslation("EntryTotal"));

        props.add("category");
        captions.add(getTranslation("Category"));
        props.add("eligibleCategories");
        captions.add(getTranslation("Registration.EligibleCategories"));

        props.add("membership");
        captions.add(getTranslation("Membership"));

        props.add("coach");
        captions.add(getTranslation("Coach"));
        props.add("custom1");
        captions.add(getTranslation("Custom1.Title"));
        props.add("custom2");
        captions.add(getTranslation("Custom2.Title"));

        props.add("lotNumber");
        captions.add(getTranslation("Lot"));
        
        props.add("federationCodes");
        captions.add(getTranslation("Registration.FederationCodes"));
        
        props.add("eligibleForIndividualRanking");
        captions.add(getTranslation("Eligible for Individual Ranking?"));

        crudFormFactory.setVisibleProperties(props.toArray(new String[0]));
        crudFormFactory.setFieldCaptions(captions.toArray(new String[0]));

        crudFormFactory.setFieldProvider("gender", new OwlcmsComboBoxProvider<>(getTranslation("Gender"),
                Arrays.asList(Gender.mfValues()), new TextRenderer<>(Gender::name), Gender::name));
        List<Group> groups = GroupRepository.findAll();
        groups.sort((Comparator<Group>) new NaturalOrderComparator<Group>());
        crudFormFactory.setFieldProvider("group", new OwlcmsComboBoxProvider<>(getTranslation("Group"),
                groups, new TextRenderer<>(Group::getName), Group::getName));
        crudFormFactory.setFieldProvider("category", new OwlcmsComboBoxProvider<>(getTranslation("Category"),
                CategoryRepository.findActive(), new TextRenderer<>(Category::getName), Category::getName, false));
        crudFormFactory.setFieldProvider("eligibleCategories",
                new OwlcmsMultiSelectComboBoxProvider<>(getTranslation("Registration.EligibleCategories"),
                        new ArrayList<Category>(), new TextRenderer<>(Category::getName), Category::getName));
//        crudFormFactory.setFieldProvider("ageDivision",
//                new OwlcmsComboBoxProvider<>(getTranslation("AgeDivision"), Arrays.asList(AgeDivision.values()),
//                        new TextRenderer<>(ad -> getTranslation("Division." + ad.name())), AgeDivision::name));

        crudFormFactory.setFieldType("bodyWeight", LocalizedDecimalField.class);
        crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class);

        // ValidationTextField (or a wrapper) must be used as workaround for unexplained
        // validation behaviour
        crudFormFactory.setFieldType("snatch1Declaration", ValidationTextField.class);
        crudFormFactory.setFieldType("cleanJerk1Declaration", ValidationTextField.class);
        crudFormFactory.setFieldType("qualifyingTotal", ValidationTextField.class);
        crudFormFactory.setFieldType("yearOfBirth", ValidationTextField.class);
    }

    private void updateURLLocation(UI ui, Location location, Group newGroup) {
        // change the URL to reflect fop group
        HashMap<String, List<String>> params = new HashMap<>(
                location.getQueryParameters().getParameters());
        if (!isIgnoreGroupFromURL() && newGroup != null) {
            params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
            if (newGroup != null) {
                params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
                currentGroup = newGroup;
                OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
                crudGrid.setCrudFormFactory(crudFormFactory);
            }
        } else {
            params.remove("group");
        }
        ui.getPage().getHistory().replaceState(null, new Location(location.getPath(), new QueryParameters(params)));
    }
    private Button cardsButton;
    private Group group;
    private ComboBox<Group> groupSelect;
    private Button startingListButton;

    /**
     * @return the groupSelect
     */
    public ComboBox<Group> getGroupSelect() {
        return groupSelect;
    }

    /**
     * @param groupSelect the groupSelect to set
     */
    public void setGroupSelect(ComboBox<Group> groupSelect) {
        this.groupSelect = groupSelect;
    }

    /**
     * Create the top bar.
     *
     * Note: the top bar is created before the content.
     *
     * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
     *
     * @param topBar
     */
    @Override
    public FlexLayout createMenuArea() {
        logger.warn("::::: creating buttons");
        setGroupSelect(new ComboBox<>());
        getGroupSelect().setPlaceholder(getTranslation("Group"));
        List<Group> groups = GroupRepository.findAll();
        groups.sort((Comparator<Group>) new NaturalOrderComparator<Group>());
        getGroupSelect().setItems(groups);
        getGroupSelect().setItemLabelGenerator(Group::getName);
        getGroupSelect().setClearButtonVisible(true);
        getGroupSelect().setValue(null);
        getGroupSelect().addValueChangeListener(e -> {
            setContentGroup(e);
        });

        Button drawLots = new Button(getTranslation("DrawLotNumbers"), (e) -> {
            drawLots();
        });

        Button deleteAthletes = new Button(getTranslation("DeleteAthletes"), (e) -> {
            new ConfirmationDialog(getTranslation("DeleteAthletes"), getTranslation("Warning_DeleteAthletes"),
                    getTranslation("Done_period"), () -> {
                        deleteAthletes();
                    }).open();

        });
        deleteAthletes.getElement().setAttribute("title", getTranslation("DeleteAthletes_forListed"));

        Button clearLifts = new Button(getTranslation("ClearLifts"), (e) -> {
            new ConfirmationDialog(getTranslation("ClearLifts"), getTranslation("Warning_ClearAthleteLifts"),
                    getTranslation("LiftsCleared"), () -> {
                        clearLifts();
                    }).open();
        });
        deleteAthletes.getElement().setAttribute("title", getTranslation("ClearLifts_forListed"));

        JXLSCards cardsWriter = new JXLSCards();
        JXLSStartingList startingListWriter = new JXLSStartingList();

        cardsButton = createCardsButton(cardsWriter);
        startingListButton = createStartingListButton(startingListWriter);

        Button resetCats = new Button(getTranslation("ResetCategories.ResetAthletes"), (e) -> {
            new ConfirmationDialog(
                    getTranslation("ResetCategories.ResetCategories"),
                    getTranslation("ResetCategories.Warning_ResetCategories"),
                    getTranslation("ResetCategories.CategoriesReset"), () -> {
                        resetCategories();
                    }).open();
        });
        resetCats.getElement().setAttribute("title", getTranslation("ResetCategories.ResetCategoriesMouseOver"));

        HorizontalLayout buttons;
        if (Config.getCurrent().featureSwitch("preCompDocs", true)) {
            buttons = new HorizontalLayout(drawLots, deleteAthletes, clearLifts,
                    resetCats);
        } else {
            buttons = new HorizontalLayout(drawLots, deleteAthletes, clearLifts, startingListButton,
                    cardsButton,
                    resetCats);
        }

        buttons.setPadding(false);
        buttons.setMargin(false);
        buttons.setSpacing(true);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

        FlexLayout topBar = new FlexLayout();
        topBar.getStyle().set("flex", "100 1");
        topBar.removeAll();
        topBar.add(getGroupSelect(), buttons);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return topBar;
    }

    protected void errorNotification() {
        Label content = new Label(getTranslation("Select_group_first"));
        content.getElement().setAttribute("theme", "error");
        Button buttonInside = new Button(getTranslation("GotIt"));
        buttonInside.getElement().setAttribute("theme", "error primary");
        VerticalLayout verticalLayout = new VerticalLayout(content, buttonInside);
        verticalLayout.setAlignItems(Alignment.CENTER);
        Notification notification = new Notification(verticalLayout);
        notification.setDuration(3000);
        buttonInside.addClickListener(event -> notification.close());
        notification.setPosition(Position.MIDDLE);
        notification.open();
    }

    protected void setContentGroup(ComponentValueChangeEvent<ComboBox<Group>, Group> e) {
        group = e.getValue();
        groupFilter.setValue(e.getValue());
    }

    private void clearLifts() {
        JPAService.runInTransaction(em -> {
            List<Athlete> athletes = (List<Athlete>) doFindAll(em);
            for (Athlete a : athletes) {
                a.clearLifts();
                em.merge(a);
            }
            em.flush();
            return null;
        });
    }

    private Button createCardsButton(JXLSCards cardsWriter) {
        String resourceDirectoryLocation = "/templates/cards";
        String title = Translator.translate("AthleteCards");
        String downloadedFilePrefix = "cards";
        DownloadDialog cardsButtonFactory = new DownloadDialog(
                () -> {
                    JXLSCards rs = new JXLSCards();
                    // group may have been edited since the page was loaded
                    rs.setGroup(group != null ? GroupRepository.getById(group.getId()) : null);
                    return rs;
                },
                resourceDirectoryLocation,
                Competition::getComputedCardsTemplateFileName,
                Competition::setCardsTemplateFileName,
                title,
                downloadedFilePrefix,
                Translator.translate("Download"));
        return cardsButtonFactory.createTopBarDownloadButton();
    }

    private Button createStartingListButton(JXLSStartingList startingListWriter) {
        String resourceDirectoryLocation = "/templates/start";
        String title = Translator.translate("StartingList");
        String downloadedFilePrefix = "startingList";

        DownloadDialog startingListFactory = new DownloadDialog(
                () -> {
                    JXLSStartingList rs = new JXLSStartingList();
                    // group may have been edited since the page was loaded
                    rs.setGroup(group != null ? GroupRepository.getById(group.getId()) : null);
                    return rs;
                },
                resourceDirectoryLocation,
                Competition::getComputedStartListTemplateFileName,
                Competition::setStartListTemplateFileName,
                title,
                downloadedFilePrefix,
                Translator.translate("Download"));
        return startingListFactory.createTopBarDownloadButton();
    }

    private void deleteAthletes() {
        JPAService.runInTransaction(em -> {
            List<Athlete> athletes = (List<Athlete>) doFindAll(em);
            for (Athlete a : athletes) {
                em.remove(a);
            }
            em.flush();
            return null;
        });
        refreshCrudGrid();
    }

    private void drawLots() {
        JPAService.runInTransaction(em -> {
            List<Athlete> toBeShuffled = AthleteRepository.doFindAll(em);
            AthleteSorter.drawLots(toBeShuffled);
            for (Athlete a : toBeShuffled) {
                em.merge(a);
            }
            em.flush();
            return null;
        });
        refreshCrudGrid();
    }

    private void resetCategories() {
        AthleteRepository.resetParticipations();
        refreshCrudGrid();
    }
}