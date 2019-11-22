package app.owlcms.components.fields;

import com.vaadin.flow.component.textfield.TextField;

/**
 * Absolutely horrible workaround for validations.
 *
 * Validations in user mode correctly sets the invalid flag, but something
 * unknown causes a second property change event to be triggered, which resets
 * the invalid indicator on the text field.¸
 *
 * We have no idea why that is.
 *
 * @author JFLamy
 *
 */
@SuppressWarnings("serial")
public class ValidationTextField extends TextField {
    private boolean invalid;

    @Override
    public void setInvalid(boolean invalid) {
        if (this.invalid && !invalid) {
            // ignore setting invalid to false if current state is false.
            // System.err.println("overriding invalid "+LoggerUtils.stackTrace());
        } else {
            super.setInvalid(invalid);
        }
        // next update from true to false will be acceped.
        this.invalid = invalid;

    }
}
