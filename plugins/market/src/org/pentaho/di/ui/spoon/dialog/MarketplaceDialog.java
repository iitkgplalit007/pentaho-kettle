/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2012 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.ui.spoon.dialog;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.market.Market;
import org.pentaho.di.core.market.entry.MarketEntries;
import org.pentaho.di.core.market.entry.MarketEntry;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.safehaus.uuid.Logger;


public class MarketplaceDialog extends Dialog {
  private static Class<?> MARKET_PKG = Market.class; // for i18n purposes, needed by Translator2!!
  
  private Label wlMarketplaces;
  private Button wClose;
  private Shell shell;
  private PropsUI props;
  private ExpandBar expandBar;
  private int margin;
  private int middle;
  private Text selectionFilter;

  private MarketEntries marketEntries = null;
  private Map<MarketEntry, Composite> marketEntryControls = new HashMap<MarketEntry, Composite>();
  
  public MarketplaceDialog(Shell parent) {
    super(parent, SWT.NONE);
    props = PropsUI.getInstance();
  }
  
  public void open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
    props.setLook(shell);
    shell.setImage(GUIResource.getInstance().getImageSpoon());

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(MARKET_PKG, "Marketplace.Dialog.Title")); 
    shell.setImage(GUIResource.getInstance().getImageLogoSmall());

    margin = Const.MARGIN;
    middle = props.getMiddlePct();

    wlMarketplaces = new Label(shell, SWT.NONE);
    wlMarketplaces.setText(BaseMessages.getString(MARKET_PKG, "Marketplace.Dialog.DetectedPlugins"));
    props.setLook(wlMarketplaces);
    FormData fdlMarketplaces = new FormData();
    fdlMarketplaces.left = new FormAttachment(0, 0);
    fdlMarketplaces.top = new FormAttachment(0, margin);
    wlMarketplaces.setLayoutData(fdlMarketplaces);
    
    selectionFilter = new Text(shell, SWT.SINGLE | SWT.BORDER | SWT.LEFT | SWT.SEARCH | SWT.ICON_SEARCH
            | SWT.ICON_CANCEL);
    selectionFilter.setFont(GUIResource.getInstance().getFontSmall());
    selectionFilter.setToolTipText(BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.Filter.Tooltip"));
    FormData fdSelectionFilter = new FormData();
    fdSelectionFilter.top = new FormAttachment(0, margin);
    fdSelectionFilter.right = new FormAttachment(100, 0);
    fdSelectionFilter.left = new FormAttachment(wlMarketplaces, 10);
    selectionFilter.setLayoutData(fdSelectionFilter);
    selectionFilter.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent arg0) {
        filter(selectionFilter.getText());
      }
    });
    
    expandBar = new ExpandBar(shell, SWT.V_SCROLL | SWT.H_SCROLL);
    FormData fdBar = new FormData();
    fdBar.left = new FormAttachment(0, 0);
    fdBar.top = new FormAttachment(wlMarketplaces, margin);
    fdBar.right = new FormAttachment(100, 0);
    fdBar.bottom = new FormAttachment(100, -50);
    expandBar.setLayoutData(fdBar);

    addMarketPlaceEntries();

    wClose = new Button(shell, SWT.PUSH);
    wClose.setText(BaseMessages.getString(MARKET_PKG, "System.Button.Close"));
    wClose.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        dispose();
      }
    });

    BaseStepDialog.positionBottomButtons(shell, new Button[] { wClose }, margin, null);

    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener(new ShellAdapter() {
      public void shellClosed(ShellEvent e) {
        dispose();
      }
    });

    BaseStepDialog.setSize(shell);

    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
  }
    
  private ExpandItem getExpandItem(final MarketEntry marketEntry) {
    //finds the expand item associated with the specified marketEntry.
    Composite composite = marketEntryControls.get(marketEntry);
    if (composite == null) return null;
    for (ExpandItem expandItem : expandBar.getItems()) {
      if (expandItem.getControl() == composite) return expandItem;
    }
    return null;
  }
  
  private ExpandItem createExpandItem(final MarketEntry marketEntry){
    ExpandItem expandItem = new ExpandItem(expandBar, SWT.NONE);
    Composite composite = marketEntryControls.get(marketEntry);
    setPluginName(expandItem, marketEntry.getName(), marketEntry.isInstalled());
    expandItem.setHeight(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y+30);
    expandItem.setControl(composite);
    Image image;
    switch (marketEntry.getType()) {
      case Step:
        image = GUIResource.getInstance().getImageTransGraph();
        break;
      case JobEntry:
        image = GUIResource.getInstance().getImageTransGraph();
        break;
      case SpoonPlugin:
        image = GUIResource.getInstance().getImageTransGraph();
        break;
      case Database:
        image = GUIResource.getInstance().getImageConnection();
        break;
      default:
        image = GUIResource.getInstance().getImageArrow();
        break;
    }
    expandItem.setImage(image);
    return expandItem;
  }
  
  private Composite createMarketEntryControl(final MarketEntry marketEntry){
    final Composite composite = new Composite(expandBar, SWT.NONE);
    FormLayout layout = new FormLayout();
    layout.marginHeight = margin;
    layout.marginWidth = margin;
    composite.setLayout(layout);
    props.setLook(composite);

    // Add a series of details in the expand-bar item as well as an install
    // button...
    //
    Control lastControl = null;
    // The ID
    addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.ID.label"), lastControl);
    lastControl = addRightLabel(composite, Const.NVL(marketEntry.getId(), ""), lastControl);
    // The name
    addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.Name.label"), lastControl);
    lastControl = addRightLabel(composite, Const.NVL(marketEntry.getName(), ""), lastControl);
    // The version
    addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.AvailableVersion.label"), lastControl);
    lastControl = addRightLabel(composite, Const.NVL(marketEntry.getVersion(), ""), lastControl);
    // The author
    if (!Const.isEmpty(marketEntry.getAuthor())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.Author.label"), lastControl);
      lastControl = addRightLabel(composite, Const.NVL(marketEntry.getAuthor(), ""), lastControl);
    }
    // Installation path
    addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.InstallPath.label"), lastControl);
    lastControl = addRightLabel(composite, new File(Market.buildPluginsFolderPath(marketEntry)).getAbsolutePath(), lastControl);
    // The description
    if (!Const.isEmpty(marketEntry.getDescription())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.Description.label"), lastControl);
      lastControl = addRightLabel(composite, marketEntry.getDescription(), lastControl);
    }
    // The package URL
    addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.PackageURL.label"), lastControl);
    lastControl = addRightURL(composite, marketEntry.getPackageUrl(), lastControl);
    // The documentation URL
    if (!Const.isEmpty(marketEntry.getDocumentationUrl())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.Documentation.label"), lastControl);
      lastControl = addRightURL(composite, marketEntry.getDocumentationUrl(), lastControl);
    }
    // The case tracking URL
    if (!Const.isEmpty(marketEntry.getCasesUrl())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.CaseTracking.label"), lastControl);
      lastControl = addRightURL(composite, marketEntry.getCasesUrl(), lastControl);
    }
    // The source code URL
    if (!Const.isEmpty(marketEntry.getSourceUrl())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.SourceCode.label"), lastControl);
      lastControl = addRightURL(composite, marketEntry.getSourceUrl(), lastControl);
    }
    // The license name
    if (!Const.isEmpty(marketEntry.getLicenseName())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.License.label"), lastControl);
      lastControl = addRightLabel(composite, Const.NVL(marketEntry.getLicenseName(), ""), lastControl);
    }
    // The license text
    if (!Const.isEmpty(marketEntry.getLicenseText())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.LicenseDetails.label"), lastControl);
      lastControl = addRightLabel(composite, Const.NVL(marketEntry.getLicenseText(), ""), lastControl);
    }
    // The support level
    addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.SupportLevel.label"), lastControl);
    lastControl = addRightLabel(composite, marketEntry.getSupportLevel().getDescription(), lastControl);
    // The support message
    if (!Const.isEmpty(marketEntry.getSupportMessage())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.SupportMessage.label"), lastControl);
      lastControl = addRightLabel(composite, marketEntry.getSupportMessage(), lastControl);
    }
    // The support URL
    if (!Const.isEmpty(marketEntry.getSupportUrl())) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.SupportURL.label"), lastControl);
      lastControl = addRightURL(composite, marketEntry.getSupportUrl(), lastControl);
    }

    Market.discoverInstalledVersion(marketEntry);

    if (marketEntry.isInstalled()) {
      addLeftLabel(composite, BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.InstalledVersion.label"), lastControl);
      lastControl = addRightLabel(composite, marketEntry.getInstalledVersion(), lastControl);
    }

    final Button installedButton = new Button(composite, SWT.PUSH);
    setButtonLabel(installedButton, marketEntry.isInstalled());
    
    // Create ProgressMonitorDialog for long-running operations
    final ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);

    // Allow for upgrade
    final Button upgradeButton = new Button(composite, SWT.PUSH);
    upgradeButton.setText(BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.UpgradeTo.button", marketEntry.getVersion()));
    //TODO: extract listener and instantiate that only once.
    upgradeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        try {
          if (marketEntry.getId().equals("market")) {
            // prompt for a confirmation
            MessageBox mb = new MessageBox(shell, SWT.NO | SWT.YES | SWT.ICON_WARNING);
            // "This file already exists.  Do you want to overwrite it?"
            mb.setMessage(BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.UpgradeMarket.Message"));
            // "This file already exists!"
            mb.setText(BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.UpgradeMarket.Title"));
            int id = mb.open();
            if (id == SWT.YES) {
              dispose();
              Market.upgradeMarket(marketEntry);
            }
          } else {
            try {
            // Create runner for upgrade and run it
              pmd.run(true, true, new ProgressMonitorRunner(new Runnable() {
                public void run() {
                  try {
                    Market.install(marketEntry, pmd);
                    upgradeButton.setEnabled(false);
                  }
                  catch(KettleException ke) {
                    new ErrorDialog(shell, BaseMessages.getString(MARKET_PKG, "Market.error"),
                        BaseMessages.getString(MARKET_PKG, "Market.installUninstall.error"), ke);
                  }
                }
              }));
          }
          catch (Exception ex) {
            new ErrorDialog(shell, "Error with Progress Monitor Dialog", "Error with Progress Monitor Dialog", ex);
          }
          }
        } catch (KettleException ke) {
          new ErrorDialog(shell, BaseMessages.getString(MARKET_PKG, "Market.error"),
              BaseMessages.getString(MARKET_PKG, "Market.installUninstall.error"), ke);
        }
      }
    });
    
    // Create runner for installation/uninstallation
    final ProgressMonitorRunner installRunner = new ProgressMonitorRunner(new Runnable() {
      public void run() {
        try {
          Market.installUninstall(marketEntry, marketEntry.isInstalled(), pmd);
          Market.discoverInstalledVersion(marketEntry);
          setButtonLabel(installedButton, marketEntry.isInstalled());
          setPluginName(marketEntry);
          if(upgradeButton != null) {
            upgradeButton.setVisible(marketEntry.isInstalled());
          }
      }
        catch(KettleException ke) {
          new ErrorDialog(shell, BaseMessages.getString(MARKET_PKG, "Market.error"),
              BaseMessages.getString(MARKET_PKG, "Market.installUninstall.error"), ke);
        }
      }
    });
    BaseStepDialog.positionBottomButtons(composite, new Button[] {installedButton,  upgradeButton}, margin, lastControl);
    boolean showUpgradeButton = marketEntry.isInstalled() && 
        ( marketEntry.getInstalledVersion()==null || 
          marketEntry.getVersion()==null || 
          marketEntry.getInstalledVersion().compareTo(marketEntry.getVersion())<0
        ) ;
    upgradeButton.setVisible(showUpgradeButton);
    
    
    
    // Create runner for market uninstallation
    final ProgressMonitorRunner uninstallMarketRunner = new ProgressMonitorRunner(new Runnable() {
      public void run() {
        try {
          Market.uninstallMarket();
      }
        catch(KettleException ke) {
          new ErrorDialog(shell, BaseMessages.getString(MARKET_PKG, "Market.error"),
              BaseMessages.getString(MARKET_PKG, "Market.installUninstall.error"), ke);
        }
      }
    });
    
    installedButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        try {
          if (marketEntry.getId().equals("market")) {
            // prompt for a confirmation
            MessageBox mb = new MessageBox(shell, SWT.NO | SWT.YES | SWT.ICON_WARNING);
            // "This file already exists.  Do you want to overwrite it?"
            mb.setMessage(BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.UninstallMarket.Message"));
            // "This file already exists!"
            mb.setText(BaseMessages.getString(MARKET_PKG, "MarketplacesDialog.UninstallMarket.Title"));
            int id = mb.open();
            if (id == SWT.YES) {
              dispose();
              pmd.run(true, true, uninstallMarketRunner);
            }
          } else {
          pmd.run(true, true, installRunner);
          }
        } catch (Throwable ke) {
          new ErrorDialog(shell, BaseMessages.getString(MARKET_PKG, "Market.error"),
              BaseMessages.getString(MARKET_PKG, "Market.installUninstall.error"), ke);
        }
      }
    });

    Label wlName = new Label(composite, SWT.LEFT);
    props.setLook(wlName);
    wlName.setText("Name:");
    FormData fdlName = new FormData();
    fdlName.top = new FormAttachment(0, 0);
    fdlName.left = new FormAttachment(0, 0);
    fdlName.right = new FormAttachment(middle, 0);
    wlName.setLayoutData(fdlName);
    return composite;
  }
  
  private void addMarketPlaceEntries(){
    //initial creating of market place entries.
    //we create and cache controls to go inside the expand items
    //these are then reused whenever we need to populate the expandbar (filtering)
    //initially, there's no filter so we create an expanditem for each entry.
    if (marketEntries != null) return;
    marketEntries = new MarketEntries();
    Composite composite;
    for (final MarketEntry marketEntry : marketEntries) {
      composite = createMarketEntryControl(marketEntry);
      marketEntryControls.put(marketEntry, composite);
      createExpandItem(marketEntry);
    }
  }

  private void setButtonLabel(Button button, boolean isInstalled) {
    String text;
    if (isInstalled) {
      text = "Marketplaces.Dialog.UninstallButton.label";
    } else {
      text = "Marketplaces.Dialog.install.button.label";
    }
    button.setText(BaseMessages.getString(MARKET_PKG, text));
  }
  
  
  
  private void setPluginName(MarketEntry marketEntry) {
    ExpandItem expandItem = getExpandItem(marketEntry);
    setPluginName(expandItem, marketEntry.getName(), marketEntry.isInstalled());
  }
  
  private void setPluginName(ExpandItem expandItem, String name, boolean isInstalled) {
    if (isInstalled) {
      expandItem.setText(BaseMessages.getString(MARKET_PKG, "Marketplaces.Dialog.PluginInstalled.message", name));
    } else {
      expandItem.setText(BaseMessages.getString(MARKET_PKG, "Marketplaces.Dialog.PluginNotInstalled.message", name));
    }
  }
	
  private void addLeftLabel(Composite composite, String string, Control lastControl) {
    Label label = new Label(composite, SWT.RIGHT);
    props.setLook(label);
    if (string == null) {
      string = "null";
    }
    label.setText(string);
    label.setFont(GUIResource.getInstance().getFontBold());
    FormData fdLabel = new FormData();
    if (lastControl!=null) {
      fdLabel.top = new FormAttachment(lastControl, 2*margin);
    } else {
      fdLabel.top = new FormAttachment(0, 0);
    }
    fdLabel.left = new FormAttachment(0, 0);
    fdLabel.right = new FormAttachment(middle/2, 0);
    label.setLayoutData(fdLabel);
  }

  private Control addRightLabel(Composite composite, String string, Control lastControl) {
    Label label = new Label(composite, SWT.LEFT);
    props.setLook(label);
    if (string == null) {
      string = "null";
    }
    label.setText(string);
    FormData fdLabel = new FormData();
    if (lastControl!=null) {
      fdLabel.top = new FormAttachment(lastControl, 2*margin);
    } else {
      fdLabel.top = new FormAttachment(0, 0);
    }
    fdLabel.left = new FormAttachment(middle/2, margin);
    fdLabel.right = new FormAttachment(100, 0);
    label.setLayoutData(fdLabel);
    
    return label;
  }
  
  private Control addRightURL(Composite composite, final String string, Control lastControl) {
    Link link = new Link(composite, SWT.LEFT);
    props.setLook(link);
    link.setText("<a>"+string+"</a>");
    FormData fdLabel = new FormData();
    if (lastControl!=null) {
      fdLabel.top = new FormAttachment(lastControl, 2*margin);
    } else {
      fdLabel.top = new FormAttachment(0, 0);
    }
    fdLabel.left = new FormAttachment(middle/2, margin);
    fdLabel.right = new FormAttachment(100, 0);
    link.setLayoutData(fdLabel);
    
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent arg0) {
        Spoon.getInstance().addSpoonBrowser(string, string);
      }
    });
    
    return link;
  }
  
  public void dispose()
	{
    clearExpandItems();
    marketEntryControls.clear();
		props.setScreen(new WindowProperty(shell));
		shell.dispose();
	}
  
  private class ProgressMonitorRunner implements IRunnableWithProgress {
	  
	  private Runnable runnable;
	  
	  public ProgressMonitorRunner(Runnable runnable) {
		  this.runnable = runnable;
	  }
	  
	  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
		{
			try
			{
				Spoon.getInstance().getDisplay().asyncExec(runnable);
			}
			catch(Exception e)
			{
				throw new InvocationTargetException(e, "Error during install: "+e.toString());
			}
		}
  }
  
  private void clearExpandItems(){
    for (ExpandItem item: expandBar.getItems()){
      item.setControl(null);
      item.dispose();
    }
  }
  
  private void filter(String filter) {
    //expand items can't be shown or hidden; they are either there or not.
    //so, we first wipe all expand items, then add only those that match the filter.
    clearExpandItems();
    String filterUCase = filter.toUpperCase();
    String name;
    boolean visible;
    for (MarketEntry marketEntry : marketEntries) {
      if (!filter.isEmpty() && !marketEntry.getName().toUpperCase().contains(filterUCase)) continue;
      createExpandItem(marketEntry);
    }
  }
}