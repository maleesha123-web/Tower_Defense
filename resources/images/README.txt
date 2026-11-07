Optional image assets
=====================
Place optional images here (e.g. background.png, logo.png).

The game does NOT require any image files: every visual has a Java2D
shape-based fallback, so missing images never cause a crash.
Load them through com.mathtowerdefense.gui.ResourceLoader (classpath first,
then resources/ folder) - never with absolute paths.
