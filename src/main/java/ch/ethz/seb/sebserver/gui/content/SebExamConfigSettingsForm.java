/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfigHistory;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SebExamConfigUndo;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SebExamConfigSettingsForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(SebExamConfigSettingsForm.class);

    private static final String VIEW_TEXT_KEY_PREFIX =
            "sebserver.examconfig.props.form.views.";
    private static final String KEY_SAVE_TO_HISTORY_SUCCESS =
            "sebserver.examconfig.action.saveToHistory.success";
    private static final String KEY_UNDO_SUCCESS =
            "sebserver.examconfig.action.undo.success";

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.props.from.title");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final ExamConfigurationService examConfigurationService;

    protected SebExamConfigSettingsForm(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser,
            final ExamConfigurationService examConfigurationService) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
        this.examConfigurationService = examConfigurationService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final EntityKey entityKey = pageContext.getEntityKey();

        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        try {

            final ConfigurationNode configNode = this.restService.getBuilder(GetExamConfigNode.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .onError(pageContext::notifyError)
                    .getOrThrow();

            final Configuration configuration = this.restService.getBuilder(GetConfigurations.class)
                    .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, configNode.getModelId())
                    .withQueryParam(Configuration.FILTER_ATTR_FOLLOWUP, Constants.TRUE_STRING)
                    .call()
                    .map(Utils::toSingleton)
                    .onError(pageContext::notifyError)
                    .getOrThrow();

            final AttributeMapping attributes = this.examConfigurationService
                    .getAttributes(configNode.templateId)
                    .onError(pageContext::notifyError)
                    .getOrThrow();

            final boolean readonly = pageContext.isReadonly() || configNode.status == ConfigurationStatus.IN_USE;
            final List<View> views = this.examConfigurationService.getViews(attributes);
            final TabFolder tabFolder = widgetFactory.tabFolderLocalized(content);
            tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            final List<ViewContext> viewContexts = new ArrayList<>();
            for (final View view : views) {
                final ViewContext viewContext = this.examConfigurationService.createViewContext(
                        pageContext,
                        configuration,
                        view,
                        attributes,
                        20,
                        readonly);
                viewContexts.add(viewContext);

                final Composite viewGrid = this.examConfigurationService.createViewGrid(
                        tabFolder,
                        viewContext);

                final TabItem tabItem = widgetFactory.tabItemLocalized(
                        tabFolder,
                        new LocTextKey(VIEW_TEXT_KEY_PREFIX + view.name));
                tabItem.setControl(viewGrid);
            }

            this.examConfigurationService.initInputFieldValues(configuration.id, viewContexts);

            final GrantCheck examConfigGrant = this.currentUser.grantCheck(EntityType.CONFIGURATION_NODE);
            this.pageService.pageActionBuilder(pageContext.clearEntityKeys())

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_SAVE_TO_HISTORY)
                    .withEntityKey(entityKey)
                    .withExec(action -> {
                        this.restService.getBuilder(SaveExamConfigHistory.class)
                                .withURIVariable(API.PARAM_MODEL_ID, configuration.getModelId())
                                .call()
                                .onError(pageContext::notifyError)
                                .getOrThrow();
                        return action;
                    })
                    .withSuccess(KEY_SAVE_TO_HISTORY_SUCCESS)
                    .publishIf(() -> examConfigGrant.iw() && !readonly)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_UNDO)
                    .withEntityKey(entityKey)
                    .withExec(action -> {
                        this.restService.getBuilder(SebExamConfigUndo.class)
                                .withURIVariable(API.PARAM_MODEL_ID, configuration.getModelId())
                                .call()
                                .onError(pageContext::notifyError)
                                .getOrThrow();
                        return action;
                    })
                    .withSuccess(KEY_UNDO_SUCCESS)
                    .publishIf(() -> examConfigGrant.iw() && !readonly)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_VIEW_PROP)
                    .withEntityKey(entityKey)
                    .ignoreMoveAwayFromEdit()
                    .publish()

            ;

        } catch (final RuntimeException e) {
            log.error("Unexpected error while trying to fetch exam configuration data and create views", e);
            throw e;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to fetch exam configuration data and create views", e);
            pageContext.notifyError(e);
        }

    }

}
