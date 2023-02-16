# DbVisualizer Extensions

The DbVisualizer team forked the **RSyntaxTextArea** repo and made changes to address two issues:
* [Incorrect text rendering with Font at 125% on Windows (#457)](https://github.com/bobbylight/RSyntaxTextArea/issues/457)
* [Slow performance when selecting a very long word (#487)](https://github.com/bobbylight/RSyntaxTextArea/issues/487)

The forked repo with the changes can be found at https://github.com/dbvis/RSyntaxTextArea/tree/dbvis

## Runtime Customization

The core problems seem to be fixed, but there are a few JVM arguments that can be used to control the behavior:

### org.fife.util.SwingUtils.textLayoutThreshold  

Defines the length of the text where **SwingUtils.drawChars()** uses a **TextLayout** to paint characters. 

Example (default):  
``-Dorg.fife.util.SwingUtils.textLayoutThreshold=1000``

### org.fife.ui.rsyntaxtextarea.BufferedTokenViewModelConverter.chunkSize  

Defines the size of chunks to use in the **BufferedTokenViewModelConverter**. By default, we don't use chunks since they distort the rendering in fractionally scaled fonts on Windows. Also, small chunk sizes distort the rendering in a non-scaled context; a chunk size of 50000 (50k) seem to work reasonably well (except for fractionally scaled fonts)

Example (default):    
``-Dorg.fife.ui.rsyntaxtextarea.BufferedTokenViewModelConverter.chunkSize=-1``
 
### org.fife.ui.rsyntaxtextarea.TokenViewModelConverter  

Defines which implementation of TokenViewModelConverter to use.

Examples:
* ``-Dorg.fife.ui.rsyntaxtextarea.TokenViewModelConverter=org.fife.ui.rsyntaxtextarea.BufferedTokenViewModelConverter`` (default)  
* ``-Dorg.fife.ui.rsyntaxtextarea.TokenViewModelConverter=org.fife.ui.rsyntaxtextarea.FixedWidthTokenViewModelConverter``
* ``-Dorg.fife.ui.rsyntaxtextarea.TokenViewModelConverter=org.fife.ui.rsyntaxtextarea.DefaultTokenViewModelConverter`` (the original implementation)
* ``-Dorg.fife.ui.rsyntaxtextarea.TokenViewModelConverter=mydomain.mypackage.MyTokenViewModelConverter`` (custom)  
  Must implement ``TokenViewModelConverter`` and a public constructor that accepts a ``RSyntaxTextArea`` and a ``TabExpander`` as arguments.  
  Example:  
  ``public class MyTokenViewModelConverter implements TokenViewModelConverter {``  
  ``public MyTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {...}``

