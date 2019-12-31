/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Collection;

import com.vaadin.flow.component.HasValue;

import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
class CategoryEditingFormFactory extends OwlcmsCrudFormFactory<Category> {
    CategoryEditingFormFactory(Class<Category> domainType) {
        super(domainType);
    }

    @Override
    public Category add(Category Category) {
        CategoryRepository.save(Category);
        return Category;
    }

    @Override
    public void delete(Category Category) {
        CategoryRepository.delete(Category);
    }

    @Override
    public Collection<Category> findAll() {
        // will not be called
        return null;
    }

    @Override
    public Category update(Category Category) {
        return CategoryRepository.save(Category);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
        binder.forField(field);
        super.bindField(field, property, propertyType);
    }

}