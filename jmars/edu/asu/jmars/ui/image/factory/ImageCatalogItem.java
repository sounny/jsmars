package edu.asu.jmars.ui.image.factory;

import java.awt.Color;
import java.util.Optional;
import static edu.asu.jmars.ui.image.factory.SvgConverter.*;

public enum ImageCatalogItem implements ImageDescriptor {	

MOUSE_POINTER_IMG("cursor.svg"),
MOUSE_POINTER_IMG_SEL("cursor_sel.svg"),	
PAN_HAND_IMG("hand.svg"),
PAN_HAND_IMG_SEL("hand_sel.svg"),
ZOOM_IN_IMG("zoomin.svg"),
ZOOM_IN_IMG_SEL("zoomin_sel.svg"),
ZOOM_OUT_IMG("zoomout.svg"),
ZOOM_OUT_IMG_SEL("zoomout_sel.svg"),
RULER_IMG("ruler.svg"),
RULER_IMG_SEL("ruler_sel.svg"),
INVESTIGATE_IMG("investigate.svg"),
INVESTIGATE_IMG_SEL("investigate_sel.svg"),
EXPORT_IMG("export.svg"),
EXPORT_IMG_SEL("export_sel.svg"),
RESIZE_IMG("resize.svg"),
RESIZE_IMG_SEL("resize_sel.svg"),
FACEBOOK_IMG("facebook.svg"),
TWITTER_IMG("twitter.svg"),
LAYER_STATUS("layerstatus.svg"),
STAR("star.svg"),
STAMPS("stamps.svg"),
LANDING_SITE_IMG("landing-site.svg"),
CRATER_COUNT_IMG("crater-counting.svg"),
SCALE_BAR_IMG("scalebar.svg"),
THREE_D_LAYER_IMG("cube2.svg"),
GRID_LAYER_IMG("grid.svg"),
GROUNDTRACK_LAYER_IMG("groundtrack.svg"),
KRC_LAYER_IMG("KRC.svg"),
MAP_LAYER_IMG("Map.svg"),
MCD_LAYER_IMG("MCD.svg"),
MOSAICS_LAYER_IMG("mosaics.svg"),
NOMENCLATURE_LAYER_IMG("nomenclature.svg"),
NORTH_LAYER_IMG("north-arrow.svg"),
ROI_LAYER_IMG("region-of-interest.svg"),
SHAPE_LAYER_IMG("shape.svg"),
STREETS_LAYER_IMG("street.svg"),
TES_LAYER_IMG("tes.svg"),
SLIDER_LAYER_IMG("timeslider.svg"),
TOGGLE_ON_IMG("toggle-on.svg"),
TOGGLE_OFF_IMG("toggle-off.svg"),
RIGHT_ARROW_IMG("arrow-right-bold.svg"),
LEFT_ARROW_IMG("arrow-left-bold.svg"),
CARET_DOWN_IMG("caret-down.svg"),
CURSOR_EXPORT("export-cursor.svg"),
CURSOR_INVESTIGATE("investigate-cursor.svg"),
CURSOR_PAN_GRAB("pangrab-cursor.svg"),
CURSOR_PAN("pan-cursor.svg"),
CURSOR_ZOOM_IN("zoomin-cursor.svg"),
CURSOR_ZOOM_OUT("zoomout-cursor.svg"),
CURSOR_RULER("ruler-cursor.svg"),
M_LOGO_IMG("map_solid.svg"),
M_OUTLINE_IMG("map_outline.svg"),
M_DISABLED_IMG("map_disabled.svg"),
P_LOGO_IMG("panner_solid.svg"),
P_OUTLINE_IMG("panner_outline.svg"),
P_DISABLED_IMG("panner_disabled.svg"),
THREED_LOGO_IMG("3d_solid.svg"),
THREED_OUTLINE_IMG("3d_outline.svg"),
THREED_DISABLED_IMG("3d_disabled.svg"),
LOCATION_IMG("location-arrow.svg"),
RIGHT_LINK_IMG("chevron-right.svg"),
ACTIVE_FILE_IMG("active-file.svg"),
EXTERNAL_LINK_IMG("external-link.svg"),
CHECK_ON_IMG("checked.svg"),
CHECK_OFF_IMG("unchecked.svg"),
FILE_DETAILS_IMG("file-details.svg"),
FILE_LIST_IMG("file-list.svg"),
COLLAPSED_IMG("collapse.svg"), //"up"
COLLAPSED_TREE("collapsed.svg"), //>
EXPANDED_IMG("expand.svg"),  //"down"
RADIO_ON_IMG("radio-on.svg"),
RADIO_OFF_IMG("radio-off.svg"),
SYNC_IMG("sync-light.svg"),
JMARS("Jicon.svg"),
PAN_1("pan_1.svg"),
PAN_2("pan_2.svg"),
PAN_5("pan_5.svg"),
PAN_10("pan_10.svg"),
PAN_E("pan_e.svg"),
PAN_N("pan_n.svg"),
PAN_NE("pan_ne.svg"),
PAN_NW("pan_nw.svg"),
PAN_S("pan_s.svg"),
PAN_SE("pan_se.svg"),
PAN_SW("pan_sw.svg"),
PAN_W("pan_w.svg"),
DOT("dot.svg"),
ADD_LAYER("add_layer.svg"),
ADDED_LAYER("added_layer.svg"),
NEW_LAYER("new-layers.svg"),
ADD_LAYER_CLOSE("addlayer_close.svg"),
ELLIPSE_MENU("ellipse-menu.svg"),
FAVORITE("favorite.svg"),
FAVORITED("favorited.svg"),
GUITAR("guitar.svg"),
GO_BACK("back-arrow.svg"),
CLOSE("close.svg"),
DETACH_DOCKED("detachdocked.svg"),
DOCK_ME("dockme.svg"),
UNDOCK_ME("undockme.svg"),
GUITAR_UNSELECTED("guitar-unselected.svg"),
INFO("info.svg"),
LIST("list.svg"),
SETTINGS("settings.svg"),
CLEAR("clear-close.svg"),
LOAD_STATUS("layerstatus.svg"),
GENERIC_LAYER("generic.svg"),
BLANK("blank.svg"),
STEP_PREV("step-backward.svg"),
STEP_NEXT("step-forward.svg"),
PLAY("play.svg"),
PAUSE("pause.svg"),
SEARCH("search-layer.svg"),
SEARCHZ("search.svg"),
INFOPANEL("infopanel.svg"),
CURSOR_COORD("mouse_coordinates.svg"),
COLOR_PICK("color_palette.svg"),
CENTER_PROJECTION("center_projection.svg"),
MAP_UPDATE("map_update.svg"),
QUICK_3D("3d.svg"),
QUICK_CHARTS("charts.svg"),
QUICK_CUSTOM_MAPS("custom_maps.svg"),
QUICK_GLOBE("globe_quick.svg"),
QUICK_VR("vr.svg"),
CONTOUR("contour.svg"),
PROFILE_IMG("profile-line.svg"),
PROFILE_PENCIL("pencil.svg"),
PROFILE_LAYER_IMG("profile-line.svg"),
TRASH("trash.svg"),
CROSSHAIRS("crosshairs.svg"),
GOTO("go.svg"),
SEARCH2("search2.svg"),
SEARCH2_SEL("search2_sel.svg"),
GOTO_LANDMARK("gotowitharrow.svg"),
COORDINATES_SWITCH("coordinates-latlon.svg"),
BOOKMARK("bookmark.svg"),
LINE("horizline.svg"),
COPY("copy.svg"),
EDIT("edit.svg"),
REMOVE("remove.svg"),
DRAG_HANDLES("drag-handles.svg"),
DRAG_DOTS("drag-dots.svg"),
EYE_VR("eye-vr.svg"),
EYE_SLASH("eye-slash.svg"),
QR("qr.svg"),

//drawing palette
DP_BRINGFORWARD("dp/bring-forward.svg"),
DP_BRINGFRONT("dp/bring-front.svg"),
DP_CIRCLE("dp/circle.svg"),
DP_ELLIPSE("dp/ellipse.svg"),
DP_5PTELLIPSE("dp/5pt-ell.svg"),
DP_FREEHAND("dp/freehand.svg"),
DP_LINE("dp/line.svg"),
DP_OBJECTEXCLUDE("dp/object-exclude.svg"),
DP_OBJECTINTERSECT("dp/object-intersect.svg"),
DP_OBJECTSUBTRACT("dp/object-subtract.svg"),
DP_OBJECTUNION("dp/object-union.svg"),
DP_SELECT("dp/select.svg"),
DP_POINT("dp/point.svg"),
DP_POLYGON("dp/polygon.svg"),
DP_SENDBACK("dp/send-back.svg"),
DP_SENDBACKWARD("dp/send-backward.svg"),
DP_SHAPESTOOLBAR("dp/shapes-toolbar.svg"),
DP_SQUARE("dp/square.svg"),
DP_CIRCLE_SEL("dp/circle-sel.svg"),
DP_ELLIPSE_SEL("dp/ellipse-sel.svg"),
DP_5PTELLIPSE_SEL("dp/5pt-ell-sel.svg"),
DP_FREEHAND_SEL("dp/freehand-sel.svg"),
DP_LINE_SEL("dp/line-sel.svg"),
DP_POINT_SEL("dp/point-sel.svg"),
DP_POLYGON_SEL("dp/polygon-sel.svg"),
DP_SQUARE_SEL("dp/square-sel.svg"),
DP_SELECT_SEL("dp/select-sel.svg"),
DP_CANCEL("dp/close.svg"),
DP_CANCEL_SEL("dp/close-sel.svg"),
DP_MULTIDRAW("dp/multidraw.svg"),
DP_MULTIDRAW_SEL("dp/multidraw-sel.svg"),
	
//expression builder
XB_HELP("xb/help.svg"),
XB_OK("xb/check.svg"),
XB_KEYBOARD("xb/keyboard-hint.svg");


	
private String imgSourceFile;	
private String PATH_TO_MATERIAL_SVG = "resources/material/images/svg/";
private Optional<Color> displayColor = Optional.empty();
private Optional<Color> strokeColor = Optional.empty();
private String conversionFormat = PNG.name();
private int width = 0;
private int height = 0;

private ImageCatalogItem(String filepath) {
	this.imgSourceFile = PATH_TO_MATERIAL_SVG + filepath;
	this.width = 0;
	this.height = 0;
	this.conversionFormat = PNG.name();
}

@Override
public String getImageFilePath() {
	return this.imgSourceFile;
}

@Override
public ImageDescriptor withDisplayColor(Color fill) {
	displayColor = Optional.ofNullable(fill);
	return this;
}

@Override
public ImageDescriptor withStrokeColor(Color stroke) {
	strokeColor = Optional.ofNullable(stroke);
	return this;
}

@Override
public ImageDescriptor as(SvgConverter format) {
	conversionFormat = format.name();
	return this;
}

@Override
public Optional<Color> getDisplayColor() {
	return displayColor;
}

@Override
public Optional<Color> getStrokeColor() {
	return strokeColor;
}

@Override
public String getConversionFormat() {
	return conversionFormat;
}

@Override
public int getWidth() {
	return width;
}

@Override
public int getHeight() {
	return height;
}

@Override
public ImageDescriptor withWidth(int w) {
	width = w;
	return this;
}

@Override
public ImageDescriptor withHeight(int h) {
	height = h;
	return this;
}

}
