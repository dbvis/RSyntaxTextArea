# DbVisualizer Extensions

The DbVisualizer team forked the **RSyntaxTextArea** repo and made changes to address two issues (changes available in GitHub at [dbvis/RSyntaxTextArea/dbvis](https://github.com/dbvis/RSyntaxTextArea/tree/dbvis)):

## [Incorrect text rendering with Font at 125% on Windows (#457)](https://github.com/bobbylight/RSyntaxTextArea/issues/457)):

In essence, the fix is to migrate RSTA to Java 11 and replace all calls to the ``Font`` and ``FontMetrics`` integer methods with calls to the floating point API introduced in Java 11.
To maintain the behavior in one place, we added ``org.fife.util.SwingUtils`` to make sure that all calls leverage the proper API.
The code in these methods are partially inspired by the internal methods in ``sun.swing.SwingUtilities2``.

Despite running the FPAPI, we observed offset errors on very long strings (+5k).
To handle this, ``SwingUtils.drawChars(...)`` checks the string length and uses ``java.awt.font.TextLayout`` if the string length exceeds 
a customizable threshold (see below) or if ``java.awt.Font.textRequiresLayout(...)`` returns true. 
Some intrinsic details can also be observed in ``sun.font.FontUtilities.isComplexText(...)``.

## [Slow performance when selecting a very long word (#487)](https://github.com/bobbylight/RSyntaxTextArea/issues/487)  
When using the mouse to click or the cursor keys to navigate very long strings, the mapping between x coordinate and the document text model takes very long time.  

For mouse-clicks, the issue is in ``org.fife.ui.rsyntaxtextarea.TokenImpl.getListOffset(...)``, and for the cursor keys the problem is in ``org.fife.ui.rsyntaxtextarea.TokenImpl.listOffsetToView(...)``.

The core issue is that the methods iterate over all characters in the string to determine the corresponding location (x-coordinate to model offset or vice versa),
calling ``java.awt.Font`` or ``java.awt.FontMetrics`` to measure the width of each character.

To address this, we extracted both methods into the new interface ``org.fife.ui.rsyntaxtextarea.TokenViewModelConverter`` and made the 
``TokenImpl`` methods call a new method ``TokenImpl.getTokenViewModelConverter(...)`` to instantiate an arbitrary implementation (see below).

### Mouse-click

The fix is to reduce the number of calls to measure characters by buffering characters between each tab character and postpone the measuring until we can measure width of the buffered string.
With the width of entire string at hand, we can check whether the clicked x is before or after the middle of the string, and then divide it in halves and recursively repeat the measurement until we end up with a single character.
On a 72k string, this reduces the number of measurements from 72,000 to 35.

Alas, on **very** wide strings (+140k) the initial measurements still take very long. We can radically reduce the time by dividing the string in chunks and measure each chunk as we go rather than waiting for the last character.  
Unfortunately, for some unknown reason this approach yields offset errors on long strings when using fractionally scaled fonts, possibly due to rounding errors.
Given more time, this is probably fixable, but for the time being the default implementation does not use chunks.
For experimental purposes, an arbitrary chunk size can be set by JVM args (see below).

#### Alternative approaches
We made several experiments to improve performance:
* **Fixed Width** (monospace fonts)  
  Using fixed widths, tabs can be easily expanded using modulus of tabsize (no need for ``TabExpander``) and we can simply divide the x-coordinate with the width of the text to get the corresponding relative offset in the model text.   
  Alas, it gets complicated with wide characters (eg Japanese) and the experimental implementation (``org.fife.ui.rsyntaxtextarea.FixedWidthTokenViewModelConverter``) yields bad offsets on very long strings.
* **Cached Character Widths**  
  Caching the widths for all characters 0-256 should cover the majority of scenarios, and we could easily refresh the cache when changing the editor font or the monitor resolution/scaling.
  Alas, a quick experiment reveals that this approach yields wrong results for long strings, even in a non-scaled monospace font; clicking the end of a 140k string offsets the cursor by 5-10 characters.
* **ViewPort**  
  An "ideal" solution would probably be to maintain a viewport where the conversion operates only on the visible part of the text; 
  if the converter knew the x offset and the model offset of the first and last visible characters on screen, the calculations would only need to consider the visible part of the screen.  
  This way, the performance would not be affected by the string size. Alas, this does not seem trivial to implement without significant changes to the current design of ``TokenImpl`` and its role in the rendering and painting?         
  Or is it possible to achieve this by inspecting the existing properties of the RSyntaxTextArea, which is passed as an argument to the methods?  
  ``textArea.getLineStartOffsetOfCurrentLine();``  
  ``textArea.getLineEndOffsetOfCurrentLine();``  
  ``textArea.getMarginLinePixelLocation();``  
  ``textArea.getMarginLinePixelLocation();``

### Cursor Keys

Fixing the cursor key problem is not as trivial; where the mouse-click solution converts a single mouse-click to a single model offset and lets us avoid calls to measure each character, the keyboard navigation moves character by character. 
We cannot simply eliminate calls to understand how far we should move the caret when we move one character left or right, and repeating this when user holds a cursor key yields a disturbing lag.
Using a timer to postpone the events until the cursor stops moving would solve this, but with the unacceptable effect that you don't see the cursor while it is moving. 

The extracted converter interface defines ``TokenViewModelConverter.listOffsetToView(...)`` to allow experiments, and the existing subclasses duplicate the standard implementation.

## Runtime Customization

The core problems seem to be fixed, but there are a few JVM arguments that can be used to control the behavior:

### org.fife.util.SwingUtils.textLayoutThreshold  

Defines the length of the text where **SwingUtils.drawChars()** uses a **TextLayout** to paint characters 
(we need this to make long strings paint correctly).

Example (default):  
``-Dorg.fife.util.SwingUtils.textLayoutThreshold=1000``

### org.fife.ui.rsyntaxtextarea.BufferedTokenViewModelConverter.chunkSize  

Defines the size of chunks to use in the **BufferedTokenViewModelConverter**. 
Using chunks improves performance when clicking on very long tokens (+70k), but by default we don't use chunks 
since they distort the rendering in fractionally scaled fonts on Windows. 
Also, small chunk sizes distort the rendering in a non-scaled context; 
a chunk size of 50000 (50k) seems to work reasonably well (except for fractionally scaled fonts).

Example (default):    
``-Dorg.fife.ui.rsyntaxtextarea.BufferedTokenViewModelConverter.chunkSize=-1``
 
### org.fife.ui.rsyntaxtextarea.TokenViewModelConverter  

Defines which implementation of **TokenViewModelConverter** to use.
There are a few built-in implementations, but there is also an option to register a custom implementation.
By default, we use the **BufferedTokenViewModelConverter** to improve performance when clicking on a very long token (+50k).

Examples:
* ``-Dorg.fife.ui.rsyntaxtextarea.TokenViewModelConverter=org.fife.ui.rsyntaxtextarea.BufferedTokenViewModelConverter`` (default)  
* ``-Dorg.fife.ui.rsyntaxtextarea.TokenViewModelConverter=org.fife.ui.rsyntaxtextarea.DefaultTokenViewModelConverter`` (the original implementation)
* ``-Dorg.fife.ui.rsyntaxtextarea.TokenViewModelConverter=org.fife.ui.rsyntaxtextarea.FixedWidthTokenViewModelConverter`` (experimental for monospace fonts)
* ``-Dorg.fife.ui.rsyntaxtextarea.TokenViewModelConverter=mydomain.mypackage.MyTokenViewModelConverter`` (custom)  
  Must implement ``TokenViewModelConverter`` and a public constructor that accepts a ``RSyntaxTextArea`` and a ``TabExpander`` as arguments.  
  Example:  
  ``public class MyTokenViewModelConverter implements TokenViewModelConverter {``  
  ``public MyTokenViewModelConverter(RSyntaxTextArea textArea, TabExpander e) {...}``
  
