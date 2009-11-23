package com.aptana.radrails.editor.common.theme.preferences;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.aptana.radrails.editor.common.theme.Theme;
import com.aptana.radrails.editor.common.theme.ThemeUtil;

public class ThemePreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{

	private static final int ROW_HEIGHT = 20;
	protected String fSelectedTheme;
	private ColorSelector fgSelector;
	private ColorSelector bgSelector;
	private ColorSelector lineHighlightSelector;
	private ColorSelector selectionSelector;
	private Combo fThemeCombo;
	private TableViewer tableViewer;
	private Set<TableEditor> fTableEditors;

	@Override
	protected Control createContents(Composite parent)
	{
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true));

		createThemeCombo(composite);
		createGlobalColorControls(composite);
		createTokenEditTable(composite);

		setTheme(ThemeUtil.getActiveTheme().getName());
		return composite;
	}

	private void createThemeCombo(Composite composite)
	{
		fThemeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		Set<String> themeNames = ThemeUtil.getThemeNames();
		for (String themeName : themeNames)
		{
			fThemeCombo.add(themeName);
		}

		fThemeCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setTheme(fThemeCombo.getText());
				super.widgetSelected(e);
			}
		});
	}

	private void createGlobalColorControls(Composite composite)
	{
		Composite colors = new Composite(composite, SWT.NONE);
		colors.setLayout(new GridLayout(4, false));

		Label label = new Label(colors, SWT.NONE);
		label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		label.setText(Messages.ThemePreferencePage_ForegroundLabel);
		fgSelector = new ColorSelector(colors);

		label = new Label(colors, SWT.NONE);
		label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		label.setText(Messages.ThemePreferencePage_SelectionLabel);
		selectionSelector = new ColorSelector(colors);

		label = new Label(colors, SWT.NONE);
		label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		label.setText(Messages.ThemePreferencePage_BackgroundLabel);
		bgSelector = new ColorSelector(colors);

		label = new Label(colors, SWT.NONE);
		label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		label.setText(Messages.ThemePreferencePage_LineHighlightLabel);
		lineHighlightSelector = new ColorSelector(colors);
	}

	private void createTokenEditTable(Composite composite)
	{
		new Label(composite, SWT.NONE);

		// TODO Hook in custom paint listeners to paint selection background color using the theme's selection color
		final Table table = new Table(composite, SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(false);
		final TableLayout layout = new TableLayout();
		table.setLayout(layout);

		table.addListener(SWT.MeasureItem, new Listener()
		{
			public void handleEvent(Event event)
			{
				event.height = ROW_HEIGHT;
			}
		});

		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(new IStructuredContentProvider()
		{

			private Theme theme;

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
			{
				this.theme = (Theme) newInput;
			}

			public void dispose()
			{

			}

			public Object[] getElements(Object inputElement)
			{
				Map<String, TextAttribute> tokens = theme.getTokens();
				return tokens.entrySet().toArray();
			}
		});
		tableViewer.setLabelProvider(new TokenLabelProvider());

		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
		final TableColumn tokenName = column.getColumn();
		tokenName.setResizable(true);
		tokenName.setText("Element"); //$NON-NLS-1$
		tokenName.setWidth(250);
		layout.addColumnData(new ColumnWeightData(50, true));
		column.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				Map.Entry<String, TextAttribute> token = (Map.Entry<String, TextAttribute>) element;
				return token.getKey();
			}

			@Override
			public Color getForeground(Object element)
			{
				Map.Entry<String, TextAttribute> token = (Map.Entry<String, TextAttribute>) element;
				Color fg = token.getValue().getForeground();
				if (fg == null)
					return new Color(Display.getCurrent(), ThemeUtil.getTheme(fSelectedTheme).getForeground());
				return fg;
			}

			@Override
			public Color getBackground(Object element)
			{
				Map.Entry<String, TextAttribute> token = (Map.Entry<String, TextAttribute>) element;
				Color bg = token.getValue().getBackground();
				if (bg == null)
					return new Color(Display.getCurrent(), ThemeUtil.getTheme(fSelectedTheme).getBackground());
				return bg;
			}

			@Override
			public Font getFont(Object element)
			{
				// FIXME show bold, italic, underline properly!
				Map.Entry<String, TextAttribute> token = (Map.Entry<String, TextAttribute>) element;
				Font font = token.getValue().getFont();
				return font;
			}
		});

		column = new TableViewerColumn(tableViewer, SWT.NONE);
		final TableColumn foreground = column.getColumn();
		foreground.setResizable(true);
		foreground.setText(Messages.ThemePreferencePage_ForegroundColumnLabel);
		foreground.setWidth(25);
		layout.addColumnData(new ColumnWeightData(5, true));
		column.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				return ""; //$NON-NLS-1$
			}
		});

		column = new TableViewerColumn(tableViewer, SWT.NONE);
		final TableColumn background = column.getColumn();
		background.setResizable(true);
		background.setText(Messages.ThemePreferencePage_BackgroundColumnLabel);
		background.setWidth(25);
		layout.addColumnData(new ColumnWeightData(5, true));
		column.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				return ""; //$NON-NLS-1$
			}
		});

		final TableColumn fontStyle = new TableColumn(table, SWT.NONE);
		fontStyle.setResizable(true);
		fontStyle.setText(Messages.ThemePreferencePage_FontStyleColumnLabel);
		fontStyle.setWidth(75);
		layout.addColumnData(new ColumnWeightData(15, true));
		
		Composite editTokenList = new Composite(composite, SWT.NONE);
		GridLayout grid = new GridLayout(2, false);
		grid.marginWidth = -3;
		editTokenList.setLayout(grid);
		
		Composite buttons = new Composite(editTokenList, SWT.NONE);
		buttons.setLayout(new RowLayout(SWT.HORIZONTAL));
		Button addToken = new Button(buttons, SWT.PUSH | SWT.FLAT);
		addToken.setText(Messages.ThemePreferencePage_AddTokenLabel);		
		Button removeToken = new Button(buttons, SWT.PUSH | SWT.FLAT);
		removeToken.setText(Messages.ThemePreferencePage_RemoveTokenLabel);
		
		Composite textField = new Composite(editTokenList, SWT.NONE);
		textField.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));
		textField.setLayout(new RowLayout(SWT.HORIZONTAL));
		Label addTokenLabel = new Label(textField, SWT.RIGHT);
		addTokenLabel.setText(Messages.ThemePreferencePage_ScopeSelectoreLabel);
		
		final Text text = new Text(textField, SWT.SINGLE);
		RowData data = new RowData();
		data.width = 250;
		text.setLayoutData(data);
		table.addSelectionListener(new SelectionListener()
		{
			
			public void widgetSelected(SelectionEvent e)
			{
				TableItem item = (TableItem) e.item;
				Map.Entry<String, TextAttribute> token = (Map.Entry<String, TextAttribute>) item.getData();
				text.setText(token.getKey());				
			}
			
			public void widgetDefaultSelected(SelectionEvent e)
			{				
			}
		});
	}

	static class TokenLabelProvider extends BaseLabelProvider implements ITableLabelProvider
	{

		public Image getColumnImage(Object element, int columnIndex)
		{
			return null;
		}

		public String getColumnText(Object element, int columnIndex)
		{
			Map.Entry<String, TextAttribute> commit = (Map.Entry<String, TextAttribute>) element;
			if (commit == null)
				return ""; //$NON-NLS-1$
			switch (columnIndex)
			{
				case 0:
					return commit.getKey();
				case 1:
					return commit.getValue().getForeground() == null ? "" : commit.getValue().getForeground() //$NON-NLS-1$
							.toString();
				case 2:
					return commit.getValue().getBackground() == null ? "" : commit.getValue().getBackground() //$NON-NLS-1$
							.toString();
				default:
					return commit.getValue().getFont() == null ? "" : commit.getValue().getFont().toString(); //$NON-NLS-1$
			}
		}

	}

	protected void setTheme(String themeName)
	{
		fSelectedTheme = themeName;
		Theme theme = ThemeUtil.getTheme(themeName);
		fgSelector.setColorValue(theme.getForeground());
		bgSelector.setColorValue(theme.getBackground());
		lineHighlightSelector.setColorValue(theme.getLineHighlight());
		selectionSelector.setColorValue(theme.getSelection());
		fThemeCombo.setText(themeName);
		tableViewer.setInput(theme);
		addCustomTableEditorControls();
	}

	private void addCustomTableEditorControls()
	{
		clearTableEditors();

		final Table table = tableViewer.getTable();
		TableItem[] items = table.getItems();
		for (int i = 0; i < items.length; i++)
		{

			Map.Entry<String, TextAttribute> commit = (Map.Entry<String, TextAttribute>) items[i].getData();
			createButton(table, items[i], 1, commit.getValue().getForeground());
			if (commit.getValue().getBackground() != null)
				createButton(table, items[i], 2, commit.getValue().getBackground());

			createFontStyle(table, items[i], commit.getValue());
		}
	}

	private void clearTableEditors()
	{
		if (fTableEditors == null)
			fTableEditors = new HashSet<TableEditor>();

		for (TableEditor tableEditor : fTableEditors)
		{
			tableEditor.getEditor().dispose();
			tableEditor.dispose();
		}
		fTableEditors.clear();
	}

	private void createFontStyle(final Table table, final TableItem item, TextAttribute text)
	{
		boolean isBold = (text.getStyle() & SWT.BOLD) != 0;
		boolean isItalic = (text.getStyle() & SWT.ITALIC) != 0;
		boolean isUnderline = (text.getStyle() & TextAttribute.UNDERLINE) != 0;
		TableEditor editor = new TableEditor(table);
		Composite buttons = new Composite(table, SWT.NONE);
		GridLayout grid = new GridLayout(3, false);
		grid.marginHeight = 0;
		grid.marginWidth = 0;
		grid.horizontalSpacing = 0;
		buttons.setLayout(grid);
		final Button b = new Button(buttons, SWT.TOGGLE | SWT.FLAT);
		b.setText(Messages.ThemePreferencePage_BoldButtonLabel);
		b.setSelection(isBold);
		final Button italic = new Button(buttons, SWT.TOGGLE | SWT.FLAT);
		italic.setText(Messages.ThemePreferencePage_ItalicButtonLabel);
		italic.setSelection(isItalic);
		final Button u = new Button(buttons, SWT.TOGGLE | SWT.FLAT);
		u.setText(Messages.ThemePreferencePage_UnderlineButtonLabel);
		u.setSelection(isUnderline);
		buttons.pack();
		editor.minimumWidth = buttons.getSize().x;
		editor.horizontalAlignment = SWT.LEFT;
		editor.setEditor(buttons, item, 3);
		fTableEditors.add(editor);

		SelectionAdapter selectionAdapter = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				Map.Entry<String, TextAttribute> token = (Map.Entry<String, TextAttribute>) item.getData();
				int style = 0;
				if (u.getSelection())
					style |= TextAttribute.UNDERLINE;
				if (b.getSelection())
					style |= SWT.BOLD;
				if (italic.getSelection())
					style |= SWT.ITALIC;
				TextAttribute at = new TextAttribute(token.getValue().getForeground(),
						token.getValue().getBackground(), style, token.getValue().getFont());
				ThemeUtil.getTheme(fSelectedTheme).update(token.getKey(), at);
				setTheme(fSelectedTheme);
			}
		};
		b.addSelectionListener(selectionAdapter);
		italic.addSelectionListener(selectionAdapter);
		u.addSelectionListener(selectionAdapter);
	}

	private void createButton(final Table table, final TableItem tableItem, final int index, final Color color)
	{
		TableEditor editor = new TableEditor(table);
		Button button = new Button(table, SWT.PUSH | SWT.FLAT);
		Image image = new Image(table.getDisplay(), 16, 16);
		GC gc = new GC(image);
		gc.setBackground(color);
		gc.fillRectangle(0, 0, 16, 16);
		gc.dispose();
		button.setImage(image);
		button.pack();
		editor.minimumWidth = button.getSize().x;
		editor.horizontalAlignment = SWT.LEFT;
		editor.setEditor(button, tableItem, index);
		fTableEditors.add(editor);

		button.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				ColorDialog colorDialog = new ColorDialog(table.getShell());
				colorDialog.setRGB(color.getRGB());
				RGB newRGB = colorDialog.open();
				if (newRGB == null)
					return;
				Map.Entry<String, TextAttribute> token = (Map.Entry<String, TextAttribute>) tableItem.getData();
				Color fg = token.getValue().getForeground();
				Color bg = token.getValue().getBackground();
				if (index == 1)
				{
					// fg
					fg = new Color(Display.getCurrent(), newRGB);
				}
				else
				{
					// bg
					bg = new Color(Display.getCurrent(), newRGB);
				}

				TextAttribute at = new TextAttribute(fg, bg, token.getValue().getStyle(), token.getValue().getFont());
				ThemeUtil.getTheme(fSelectedTheme).update(token.getKey(), at);
				setTheme(fSelectedTheme);
			}
		});
	}

	public void init(IWorkbench workbench)
	{

	}

	@Override
	public boolean performOk()
	{
		// FIXME Save any changes to the theme permanently!
		ThemeUtil.setActiveTheme(ThemeUtil.getTheme(fSelectedTheme));
		return super.performOk();
	}

}
