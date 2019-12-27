/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.results.Resource;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AgeGroupContent.
 *
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/agegroup", layout = AgeGroupLayout.class)
public class AgeGroupContent extends VerticalLayout implements CrudListener<AgeGroup>, OwlcmsContent, RequireLogin {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
//    private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
    private TextField nameFilter = new TextField();
    private Checkbox activeFilter = new Checkbox();
    private OwlcmsRouterLayout routerLayout;
    private OwlcmsCrudFormFactory<AgeGroup> ageGroupEditingFormFactory;
    private FlexLayout topBar;
    private ComboBox<Resource> ageGroupDefinitionSelect;
    private GridCrud<AgeGroup> crud;

    /**
     * Instantiates the ageGroup crudGrid.
     */
    public AgeGroupContent() {
        OwlcmsCrudFormFactory<AgeGroup> editingFormFactory = new AgeGroupEditingFormFactory(AgeGroup.class, this);
        setAgeGroupEditingFormFactory(editingFormFactory);
        crud = createGrid(getAgeGroupEditingFormFactory());
        defineFilters(crud);
        fillHW(crud, this);
    }

    @Override
    public AgeGroup add(AgeGroup ageGroup) {
        return getAgeGroupEditingFormFactory().add(ageGroup);
    }

    @Override
    public void delete(AgeGroup domainObjectToDelete) {
        getAgeGroupEditingFormFactory().delete(domainObjectToDelete);
    }

    /**
     * The refresh button on the toolbar
     *
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<AgeGroup> findAll() {
        List<AgeGroup> all = AgeGroupRepository.findFiltered(nameFilter.getValue(), (Gender) null, ageDivisionFilter.getValue(),
                (Integer) null, activeFilter.getValue(), -1, -1);
        all.sort((ag1,ag2)->{
            int compare = 0;
            compare = ObjectUtils.compare(ag1.getAgeDivision(),ag2.getAgeDivision());
            if (compare != 0) return -compare; // DEFAULT first.
            compare = ObjectUtils.compare(ag1.getGender(),ag2.getGender());
            if (compare != 0) return compare;
            compare = ObjectUtils.compare(ag1.getMinAge(),ag2.getMinAge());
            if (compare != 0) return compare;
            compare = ObjectUtils.compare(ag1.getMaxAge(),ag2.getMaxAge());
            if (compare != 0) return compare;
            return 0;
        });
        return all;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Preparation_AgeGroups");
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Override
    public AgeGroup update(AgeGroup domainObjectToUpdate) {
        return getAgeGroupEditingFormFactory().update(domainObjectToUpdate);
    }

    /**
     * The columns of the crudGrid
     *
     * @param crudFormFactory what to call to create the form for editing an athlete
     * @return
     */
    protected GridCrud<AgeGroup> createGrid(OwlcmsCrudFormFactory<AgeGroup> crudFormFactory) {
        Grid<AgeGroup> grid = new Grid<>(AgeGroup.class, false);
        grid.addColumn(new ComponentRenderer<>(cat -> {
            // checkbox to avoid entering in the form
            Checkbox activeBox = new Checkbox("Name");
            activeBox.setLabel(null);
            activeBox.getElement().getThemeList().set("secondary", true);
            activeBox.setValue(cat.isActive());
            activeBox.addValueChangeListener(click -> {
                activeBox.setValue(click.getValue());
                cat.setActive(click.getValue());
                AgeGroupRepository.save(cat);
                grid.getDataProvider().refreshItem(cat);
            });
            return activeBox;
        })).setHeader(getTranslation("Active")).setWidth("0");
        grid.addColumn(AgeGroup::getName).setHeader(getTranslation("Name"));
        grid.addColumn(new TextRenderer<AgeGroup>(
                item -> getTranslation("Division." + item.getAgeDivision().name())))
                .setHeader(getTranslation("AgeDivision"));
        grid.addColumn(AgeGroup::getGender).setHeader(getTranslation("Gender"));
        grid.addColumn(AgeGroup::getMinAge).setHeader(getTranslation("MinimumAge"));
        grid.addColumn(AgeGroup::getMaxAge).setHeader(getTranslation("MaximumAge"));
        grid.addColumn(AgeGroup::getCategoriesAsString).setAutoWidth(true)
                .setHeader(getTranslation("BodyWeightCategories"));

        crud = new OwlcmsCrudGrid<>(AgeGroup.class, new OwlcmsGridLayout(AgeGroup.class),
                crudFormFactory, grid);
        crud.setCrudListener(this);
        crud.setClickRowToUpdate(true);
        return crud;
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
    protected void createTopBar() {
        // show arrow but close menu
        getAppLayout().setMenuVisible(true);
        getAppLayout().closeDrawer();

        topBar = getAppLayout().getAppBarElementWrapper();

        Button resetCats = new Button(getTranslation("ResetCategories.ResetCategories"), (e) -> {
            new ConfirmationDialog(getTranslation("ClearLifts"),
                    getTranslation("ResetCategories.Warning_ResetCategories"),
                    getTranslation("ResetCategories.CategoriesReset"), () -> {
                        resetCategories();
                    }).open();
        });
        resetCats.getElement().setAttribute("title", getTranslation("ResetCategories.ResetCategoriesMouseOver"));
        HorizontalLayout resetButton = new HorizontalLayout(resetCats);
        resetButton.setMargin(true);

        ageGroupDefinitionSelect = new ComboBox<>();
        ageGroupDefinitionSelect.setPlaceholder(getTranslation("ResetCategories.AvailableDefinitions"));
        List<Resource> resourceList = new ResourceWalker().getResourceList("/config",
                ResourceWalker::relativeName, "AgeGroups");
        ageGroupDefinitionSelect.setItems(resourceList);
        ageGroupDefinitionSelect.setValue(null);
        ageGroupDefinitionSelect.setWidth("15em");
        ageGroupDefinitionSelect.getStyle().set("margin-left", "1em");
        setTemplateSelectionListener(resourceList);

        Button reload = new Button(getTranslation("ResetCategories.ReloadAgeGroups"), (e) -> {
            new ConfirmationDialog(getTranslation("ClearLifts"),
                    getTranslation("ResetCategories.Warning_ResetCategories"),
                    getTranslation("ResetCategories.CategoriesReset"), () -> {
                        AgeGroupRepository
                                .reloadDefinitions(ageGroupDefinitionSelect.getValue().getFileName());
                        resetCategories();
                    }).open();
        });
        HorizontalLayout reloadDefinition = new HorizontalLayout(ageGroupDefinitionSelect, reload);
        reloadDefinition.setAlignItems(FlexComponent.Alignment.BASELINE);
        reloadDefinition.setMargin(true);
        reloadDefinition.setSpacing(false);

//        topBar.getStyle().set("flex", "100 1");
        topBar.add(resetButton,reloadDefinition);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
    }

    private void resetCategories() {
        JPAService.runInTransaction(em -> {
            List<Athlete> athletes = (List<Athlete>) AthleteRepository.doFindAll(em);
            for (Athlete a : athletes) {
                List<Category> categories = CategoryRepository.findByGenderAgeBW(a.getGender(), a.getAge(),
                        a.getBodyWeight());
                a.setCategory(categories.isEmpty() ? null : categories.get(0));
                em.merge(a);
            }
            em.flush();
            return null;
        });
        crud.refreshGrid();
    }

    private void setTemplateSelectionListener(List<Resource> resourceList) {
        try {
            String curTemplateName = Competition.getCurrent().getProtocolFileName();
            Resource found = searchMatch(resourceList, curTemplateName);
            ageGroupDefinitionSelect.addValueChangeListener((e) -> {
                Competition.getCurrent().setProtocolFileName(e.getValue().getFileName());
                CompetitionRepository.save(Competition.getCurrent());
            });
            ageGroupDefinitionSelect.setValue(found);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
        Resource found = null;
        for (Resource curResource : resourceList) {
            String fileName = curResource.getFileName();
            if (fileName.equals(curTemplateName)) {
                found = curResource;
                break;
            }
        }
        return found;
    }

    /**
     * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        createTopBar();
        Competition competition = Competition.getCurrent();
        competition.computeGlobalRankings();
    }

    /**
     * The filters at the top of the crudGrid
     *
     * @param crudGrid the crudGrid that will be filtered.
     */
    protected void defineFilters(GridCrud<AgeGroup> crud) {
        nameFilter.setPlaceholder(getTranslation("Name"));
        nameFilter.setClearButtonVisible(true);
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(nameFilter);

        ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision"));
        ageDivisionFilter.setItems(AgeDivision.findAll());
        ageDivisionFilter.setItemLabelGenerator((ad) -> getTranslation("Division." + ad.name()));
        ageDivisionFilter.setClearButtonVisible(true);
        ageDivisionFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(ageDivisionFilter);
        crud.getCrudLayout().addToolbarComponent(new Label(""));

        activeFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        activeFilter.setLabel(getTranslation("Active"));
        activeFilter.setAriaLabel(getTranslation("ActiveCategoriesOnly"));
        crud.getCrudLayout().addFilterComponent(activeFilter);

        Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
        clearFilters.addClickListener(event -> {
            ageDivisionFilter.clear();
        });
        crud.getCrudLayout().addFilterComponent(clearFilters);
    }

    private OwlcmsCrudFormFactory<AgeGroup> getAgeGroupEditingFormFactory() {
        return ageGroupEditingFormFactory;
    }

    private void setAgeGroupEditingFormFactory(OwlcmsCrudFormFactory<AgeGroup> ageGroupEditingFormFactory) {
        this.ageGroupEditingFormFactory = ageGroupEditingFormFactory;
    }

    void closeDialog() {
        crud.getCrudLayout().hideForm();
        crud.getGrid().asSingleSelect().clear();
    }
}
