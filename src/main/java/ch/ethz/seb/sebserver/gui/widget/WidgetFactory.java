/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.POLYGLOT_WIDGET_FUNCTION_KEY;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.DefaultPageLayout;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;

@Lazy
@Service
@GuiProfile
public class WidgetFactory {

    private static final Logger log = LoggerFactory.getLogger(WidgetFactory.class);

    public static final int TEXT_AREA_INPUT_MIN_HEIGHT = 50;
    public static final int TEXT_INPUT_MIN_HEIGHT = 24;

    public enum ImageIcon {
        MAXIMIZE("maximize.png"),
        MINIMIZE("minimize.png"),
        ADD("add.png"),
        REMOVE("remove.png"),
        ADD_BOX("add_box.png"),
        REMOVE_BOX("remove_box.png"),
        EDIT("edit.png"),
        EDIT_SETTINGS("settings.png"),
        TEST("test.png"),
        IMPORT("import.png"),
        CANCEL("cancel.png"),
        CANCEL_EDIT("cancelEdit.png"),
        SHOW("show.png"),
        ACTIVE("active.png"),
        INACTIVE("inactive.png"),
        TOGGLE_ON("toggle_on.png"),
        TOGGLE_OFF("toggle_off.png"),
        YES("yes.png"),
        NO("no.png"),
        SAVE("save.png"),
        EXPORT("export.png"),
        SECURE("secure.png"),
        NEW("new.png"),
        DELETE("delete.png"),
        SEARCH("lens.png"),
        UNDO("undo.png"),
        COLOR("color.png");

        private String fileName;
        private ImageData image = null;

        private ImageIcon(final String fileName) {
            this.fileName = fileName;
        }

        public Image getImage(final Device device) {
            if (this.image == null) {
                try {
                    final InputStream resourceAsStream =
                            WidgetFactory.class.getResourceAsStream("/static/images/" + this.fileName);
                    this.image = new ImageData(resourceAsStream);
                } catch (final Exception e) {
                    log.error("Failed to load resource image: {}", this.fileName, e);
                }
            }

            return new Image(device, this.image);
        }
    }

    public enum CustomVariant {
        TEXT_H1("h1"),
        TEXT_H2("h2"),
        TEXT_H3("h3"),
        IMAGE_BUTTON("imageButton"),
        TEXT_ACTION("action"),
        TEXT_READONLY("readonlyText"),

        FORM_CENTER("form-center"),
        SELECTION("selection"),
        SELECTED("selected"),

        ACTIVITY_TREE_SECTION("treesection"),

        FOOTER("footer"),
        TITLE_LABEL("head"),

        MESSAGE("message"),
        ERROR("error"),
        WARNING("warning"),
        CONFIG_INPUT_READONLY("inputreadonly")

        ;

        public final String key;

        private CustomVariant(final String key) {
            this.key = key;
        }
    }

    private final PolyglotPageService polyglotPageService;
    private final I18nSupport i18nSupport;
    private final ServerPushService serverPushService;

    public WidgetFactory(
            final PolyglotPageService polyglotPageService,
            final ServerPushService serverPushService) {

        this.polyglotPageService = polyglotPageService;
        this.i18nSupport = polyglotPageService.getI18nSupport();
        this.serverPushService = serverPushService;
    }

    public I18nSupport getI18nSupport() {
        return this.i18nSupport;
    }

    public Composite defaultPageLayout(final Composite parent) {
        final Composite content = new Composite(parent, SWT.NONE);
        final GridLayout contentLayout = new GridLayout();
        contentLayout.marginLeft = 10;
        content.setLayout(contentLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(gridData);
        return content;
    }

    public Composite defaultPageLayout(final Composite parent, final LocTextKey title) {
        final Composite defaultPageLayout = defaultPageLayout(parent);
        final Label labelLocalizedTitle = labelLocalizedTitle(defaultPageLayout, title);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        labelLocalizedTitle.setLayoutData(gridData);
        return defaultPageLayout;
    }

    public Composite defaultPageLayout(
            final Composite parent,
            final LocTextKey title,
            final ActionDefinition actionDefinition) {

        final Composite defaultPageLayout = defaultPageLayout(parent);
        final Label labelLocalizedTitle = labelLocalizedTitle(defaultPageLayout, title);
        labelLocalizedTitle.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        return defaultPageLayout;
    }

    public Composite formGrid(final Composite parent, final int rows) {
        final Composite grid = new Composite(parent, SWT.NONE);
        final GridLayout layout = new GridLayout(rows, true);
        layout.horizontalSpacing = 10;
        layout.verticalSpacing = 10;
        layout.marginBottom = 10;
        layout.marginLeft = 10;
        layout.marginTop = 0;
        grid.setLayout(layout);
        grid.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        return grid;
    }

    /** Use this to create a scrolled Composite for usual popup forms
     *
     * @param parent The parent Composite
     * @return the scrolled Composite to add the form content */
    public Composite createPopupScrollComposite(final Composite parent) {
        final Composite grid = PageService.createManagedVScrolledComposite(
                parent,
                scrolledComposite -> {
                    final Composite g = new Composite(scrolledComposite, SWT.NONE);
                    g.setLayout(new GridLayout());
                    g.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
                    return g;
                },
                false);
        return grid;
    }

    public Composite createWarningPanel(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        final GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.marginWidth = 20;
        gridLayout.marginHeight = 20;
        composite.setLayout(gridLayout);
        composite.setData(RWT.CUSTOM_VARIANT, CustomVariant.WARNING.key);
        return composite;
    }

    public Button buttonLocalized(final Composite parent, final String locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(button, new LocTextKey(locTextKey));
        return button;
    }

    public Button buttonLocalized(final Composite parent, final LocTextKey locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(button, locTextKey);
        return button;
    }

    public Button buttonLocalized(final Composite parent, final CustomVariant variant, final String locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(button, new LocTextKey(locTextKey));
        button.setData(RWT.CUSTOM_VARIANT, variant.key);
        return button;
    }

    public Button buttonLocalized(
            final Composite parent,
            final int type,
            final LocTextKey locTextKey,
            final LocTextKey toolTipKey) {

        final Button button = new Button(parent, type);
        this.polyglotPageService.injectI18n(button, locTextKey, toolTipKey);
        return button;
    }

    public Label label(final Composite parent, final String text) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        return label;
    }

    public Label labelLocalized(final Composite parent, final String locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(label, new LocTextKey(locTextKey));
        return label;
    }

    public Label labelLocalized(final Composite parent, final LocTextKey locTextKey, final String defaultText) {
        final Label label = new Label(parent, SWT.NONE);
        if (this.i18nSupport.hasText(locTextKey)) {
            this.polyglotPageService.injectI18n(label, locTextKey);
        } else {
            label.setText(defaultText);
        }
        return label;
    }

    public Label labelLocalized(final Composite parent, final LocTextKey locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(label, locTextKey);
        return label;
    }

    public Label labelLocalized(final Composite parent, final CustomVariant variant, final LocTextKey locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(label, locTextKey);
        label.setData(RWT.CUSTOM_VARIANT, variant.key);
        return label;
    }

    public Label labelLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final LocTextKey locToolTextKey) {

        final Label label = new Label(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(label, locTextKey, locToolTextKey);
        return label;
    }

    public Label labelLocalized(
            final Composite parent,
            final CustomVariant variant,
            final LocTextKey locTextKey,
            final LocTextKey locToolTextKey) {

        final Label label = new Label(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(label, locTextKey, locToolTextKey);
        label.setData(RWT.CUSTOM_VARIANT, variant.key);
        return label;
    }

    public Label labelLocalizedTitle(final Composite content, final LocTextKey locTextKey) {
        final Label labelLocalized = labelLocalized(content, CustomVariant.TEXT_H1, locTextKey);
        labelLocalized.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, true, false));
        return labelLocalized;
    }

    public Text textInput(final Composite content) {
        return textInput(content, false, false);
    }

    public Text textLabel(final Composite content) {
        return textInput(content, false, true);
    }

    public Text passwordInput(final Composite content) {
        return textInput(content, true, false);
    }

    public Text textAreaInput(final Composite content, final boolean readonly) {
        return readonly
                ? new Text(content, SWT.LEFT | SWT.MULTI)
                : new Text(content, SWT.LEFT | SWT.BORDER | SWT.MULTI);
    }

    public Text textInput(final Composite content, final boolean password, final boolean readonly) {
        return readonly
                ? new Text(content, SWT.LEFT)
                : new Text(content, (password)
                        ? SWT.LEFT | SWT.BORDER | SWT.PASSWORD
                        : SWT.LEFT | SWT.BORDER);
    }

    public Text numberInput(final Composite content, final Consumer<String> numberCheck) {
        return numberInput(content, numberCheck, false);
    }

    public Text numberInput(final Composite content, final Consumer<String> numberCheck, final boolean readonly) {
        if (readonly) {
            return new Text(content, SWT.RIGHT | SWT.READ_ONLY);
        }

        final Text numberInput = new Text(content, SWT.RIGHT | SWT.BORDER);
        if (numberCheck != null) {
            numberInput.addListener(SWT.Verify, event -> {
                final String value = event.text;
                try {
                    numberCheck.accept(value);
                } catch (final Exception e) {
                    event.doit = false;
                }
            });
        }
        return numberInput;
    }

    public Group groupLocalized(
            final Composite parent,
            final int columns,
            final LocTextKey locTextKey) {

        return groupLocalized(parent, columns, locTextKey, null);
    }

    public Group groupLocalized(
            final Composite parent,
            final int columns,
            final LocTextKey locTextKey,
            final LocTextKey locTooltipKey) {

        final Group group = new Group(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(columns, true);
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        group.setLayout(gridLayout);

        this.polyglotPageService.injectI18n(group, locTextKey, locTooltipKey);
        return group;
    }

    public Tree treeLocalized(final Composite parent, final int style) {
        final Tree tree = new Tree(parent, style);
        this.polyglotPageService.injectI18n(tree);
        return tree;
    }

    public TreeItem treeItemLocalized(final Tree parent, final String locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(item, new LocTextKey(locTextKey));
        return item;
    }

    public TreeItem treeItemLocalized(final Tree parent, final LocTextKey locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(item, locTextKey);
        return item;
    }

    public TreeItem treeItemLocalized(final TreeItem parent, final String locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(item, new LocTextKey(locTextKey));
        return item;
    }

    public TreeItem treeItemLocalized(final TreeItem parent, final LocTextKey locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(item, locTextKey);
        return item;
    }

    public Table tableLocalized(final Composite parent) {
        final Table table = new Table(parent, SWT.SINGLE | SWT.NO_SCROLL);
        this.polyglotPageService.injectI18n(table);
        return table;
    }

    public Table tableLocalized(final Composite parent, final int style) {
        final Table table = new Table(parent, style);
        this.polyglotPageService.injectI18n(table);
        return table;
    }

    public TableColumn tableColumnLocalized(
            final Table table,
            final LocTextKey locTextKey) {

        return tableColumnLocalized(table, locTextKey, null);
    }

    public TableColumn tableColumnLocalized(
            final Table table,
            final LocTextKey locTextKey,
            final LocTextKey toolTipKey) {

        final TableColumn tableColumn = new TableColumn(table, SWT.NONE);
        this.polyglotPageService.injectI18n(tableColumn, locTextKey, toolTipKey);
        return tableColumn;
    }

    public TabFolder tabFolderLocalized(final Composite parent) {
        final TabFolder tabs = new TabFolder(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(tabs);
        return tabs;
    }

    public TabItem tabItemLocalized(
            final TabFolder parent,
            final LocTextKey locTextKey) {

        return this.tabItemLocalized(parent, locTextKey, null);
    }

    public TabItem tabItemLocalized(
            final TabFolder parent,
            final LocTextKey locTextKey,
            final LocTextKey toolTipKey) {

        final TabItem tabItem = new TabItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(tabItem, locTextKey, toolTipKey);
        return tabItem;
    }

    public Label labelSeparator(final Composite parent) {
        final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        final GridData data = new GridData(SWT.FILL, SWT.TOP, true, true);
        label.setLayoutData(data);
        return label;
    }

    public Label imageButton(
            final ImageIcon type,
            final Composite parent,
            final LocTextKey toolTip,
            final Listener listener) {

        final Label imageButton = labelLocalized(parent, (LocTextKey) null, toolTip);
        imageButton.setData(RWT.CUSTOM_VARIANT, CustomVariant.IMAGE_BUTTON.name());
        imageButton.setImage(type.getImage(parent.getDisplay()));
        if (listener != null) {
            imageButton.addListener(SWT.MouseDown, listener);
        }
        return imageButton;
    }

    public Selection selectionLocalized(
            final Selection.Type type,
            final Composite parent,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        return this.selectionLocalized(type, parent, itemsSupplier, null, null);
    }

    public Selection selectionLocalized(
            final Selection.Type type,
            final Composite parent,
            final Supplier<List<Tuple<String>>> itemsSupplier,
            final LocTextKey toolTipTextKey) {

        return this.selectionLocalized(type, parent, itemsSupplier, toolTipTextKey, null);
    }

    public Selection selectionLocalized(
            final Selection.Type type,
            final Composite parent,
            final Supplier<List<Tuple<String>>> itemsSupplier,
            final LocTextKey toolTipTextKey,
            final Supplier<List<Tuple<String>>> itemsToolTipSupplier) {

        return selectionLocalized(type, parent, itemsSupplier, toolTipTextKey, itemsToolTipSupplier, null);
    }

    public Selection selectionLocalized(
            final Selection.Type type,
            final Composite parent,
            final Supplier<List<Tuple<String>>> itemsSupplier,
            final LocTextKey toolTipTextKey,
            final Supplier<List<Tuple<String>>> itemsToolTipSupplier,
            final String actionLocTextPrefix) {

        final Selection selection;
        switch (type) {
            case SINGLE:
                selection = new SingleSelection(parent, SWT.READ_ONLY);
                break;
            case SINGLE_COMBO:
                selection = new SingleSelection(parent, SWT.NONE);
                break;
            case RADIO:
                selection = new RadioSelection(parent);
                break;
            case MULTI:
                selection = new MultiSelection(parent);
                break;
            case MULTI_COMBO:
                selection = new MultiSelectionCombo(
                        parent,
                        this,
                        actionLocTextPrefix,
                        // NOTE parent would work for firefox but on IE and Chrome only parent.getParent().getParent() works
                        parent.getParent().getParent());
                break;
            case MULTI_CHECKBOX:
                selection = new MultiSelectionCheckbox(parent);
                break;
            case COLOR:
                selection = new ColorSelection(parent, this, actionLocTextPrefix);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Selection.Type: " + type);
        }

        if (itemsSupplier != null) {
            final Consumer<Selection> updateFunction = ss -> {
                try {
                    ss.applyNewMapping(itemsSupplier.get());
                    if (toolTipTextKey != null) {
                        ss.setToolTipText(this.i18nSupport.getText(toolTipTextKey));
                    }
                    if (itemsToolTipSupplier != null) {
                        ss.applyToolTipsForItems(itemsToolTipSupplier.get());
                    }
                } catch (final Exception e) {
                    log.error("Unexpected error while trying to apply localization to selection widget", e);
                }
            };
            selection.adaptToControl().setData(POLYGLOT_WIDGET_FUNCTION_KEY, updateFunction);
            updateFunction.accept(selection);
        }

        return selection;
    }

    public ThresholdList thresholdList(
            final Composite parent,
            final Composite updateAnchor,
            final Collection<Threshold> values) {

        final ThresholdList thresholdList = new ThresholdList(parent, updateAnchor, this);
        if (values != null) {
            thresholdList.setThresholds(values);
        }
        return thresholdList;
    }

    public ImageUploadSelection logoImageUploadLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final boolean readonly) {

        return imageUploadLocalized(
                parent,
                locTextKey,
                readonly,
                DefaultPageLayout.LOGO_IMAGE_MAX_WIDTH,
                DefaultPageLayout.LOGO_IMAGE_MAX_HEIGHT);
    }

    public ImageUploadSelection imageUploadLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final boolean readonly,
            final int maxWidth,
            final int maxHeight) {

        final ImageUploadSelection imageUpload = new ImageUploadSelection(
                parent,
                this.serverPushService,
                this.i18nSupport,
                readonly,
                maxWidth,
                maxHeight);

        this.polyglotPageService.injectI18n(imageUpload, locTextKey);
        return imageUpload;
    }

    public FileUploadSelection fileUploadSelection(
            final Composite parent,
            final boolean readonly,
            final Collection<String> supportedFiles) {

        final FileUploadSelection fileUploadSelection =
                new FileUploadSelection(parent, this.i18nSupport, readonly);

        if (supportedFiles != null) {
            supportedFiles.forEach(ext -> fileUploadSelection.withSupportFor(ext));
        }
        return fileUploadSelection;
    }

}
