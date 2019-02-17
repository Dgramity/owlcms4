/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.preparation;

import org.ledocte.owlcms.ui.home.MainNavigationLayout;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.vaadin.flow.component.html.Label;

/**
 * The Class CategoryLayout.
 */
@SuppressWarnings("serial")
public class CategoryLayout extends MainNavigationLayout {

	/* (non-Javadoc)
	 * @see org.ledocte.owlcms.ui.home.MainNavigationLayout#createAppLayoutInstance()
	 */
	@Override
	public AppLayout createAppLayoutInstance() {
		AppLayout appLayout = super.createAppLayoutInstance();
		appLayout.setTitleComponent(new Label("Edit Categories"));
		return appLayout;
		
	}
	
	

}
