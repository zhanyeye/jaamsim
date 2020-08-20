/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 */
package com.jaamsim.DisplayModels;

import java.util.ArrayList;
import java.util.List;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Tag;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.VisibilityInfo;

public class SiloModel extends DisplayModel {

	private static ArrayList<Vec4d> top;
	private static ArrayList<Vec4d> middle;
	private static ArrayList<Vec4d> bottom;

	static {
		List<Vec4d> circlePoints = RenderUtils.getCirclePoints(32);

		top = new ArrayList<>();
		middle = new ArrayList<>();
		bottom = new ArrayList<>();

		for (int i = 0; i < circlePoints.size(); ++i) {
			top.add( new Vec4d( circlePoints.get(i).getByInd(0) * 0.5, circlePoints.get(i).getByInd(1) * 0.5, 0.5, 1.0d ) );
			middle.add( new Vec4d( circlePoints.get(i).getByInd(0) * 0.5, circlePoints.get(i).getByInd(1) * 0.5, 0.0, 1.0d ) );
			bottom.add( new Vec4d( circlePoints.get(i).getByInd(0) * 0.15, circlePoints.get(i).getByInd(1) * 0.15, -0.5, 1.0d ) );
		}
	}


	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return ent instanceof DisplayEntity;
	}

	private class Binding extends DisplayModelBinding {

		private DisplayEntity siloObservee;

		private ArrayList<RenderProxy> cachedProxies;

		private Transform transCache;
		private Vec3d scaleCache;
		private double ratioCache;
		private Color4d contentsColorCache;
		private Color4d outlineColorCache;
		private VisibilityInfo viCache;


		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				siloObservee = (DisplayEntity)observee;
			} catch (ClassCastException e) {
				// The observee is not a display entity
				siloObservee = null;
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {

			if (siloObservee == null || !siloObservee.getShow()) {
				return;
			}

			Transform trans = siloObservee.getGlobalTrans();
			Vec3d scale = siloObservee.getSize();
			scale.mul3(getModelScale());

			double ratio = 0.0;

			Color4d contentsColor = null;

			Color4d outlineColor = ColourInput.BLACK;
			Tag outlineTag = siloObservee.getTagSet().get(ShapeModel.TAG_OUTLINES);
			if (outlineTag != null && outlineTag.colors != null && outlineTag.colors.length > 0)
				outlineColor = outlineTag.colors[0];

			Color4d backgroundColor = ColourInput.LIGHT_GREY;
			Tag bodyTag = siloObservee.getTagSet().get(ShapeModel.TAG_BODY);
			if (bodyTag != null && bodyTag.colors != null && bodyTag.colors.length > 0)
				backgroundColor = bodyTag.colors[0];

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compare(transCache, trans);
			dirty = dirty || dirty_vec3d(scaleCache, scale);
			dirty = dirty || ratioCache != ratio;
			dirty = dirty || dirty_col4d(contentsColorCache, contentsColor);
			dirty = dirty || dirty_col4d(outlineColorCache, outlineColor);
			dirty = dirty || !compare(viCache, vi);

			transCache = trans;
			scaleCache = scale;
			ratioCache = ratio;
			contentsColorCache = contentsColor;
			outlineColorCache = outlineColor;
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				registerCacheHit("Silo");
				out.addAll(cachedProxies);
				return;
			}
			registerCacheMiss("Silo");

			cachedProxies = new ArrayList<>();


			long pickingID = siloObservee.getEntityNumber();

			// Determine points for contents level
			ArrayList<Vec4d> contentsLevel = new ArrayList<>();
			for (int i = 0; i < top.size(); ++i) {
				if( ratio < 0.5 ) {
					double botX = bottom.get(i).getByInd(0);
					double topX = top.get(i).getByInd(0);
					double botY = bottom.get(i).getByInd(1);
					double topY = top.get(i).getByInd(1);
					contentsLevel.add( new Vec4d( (topX-botX)*ratio*2.0 + botX, (topY-botY)*ratio*2.0 + botY, ratio - 0.5, 1.0d ) );
				}
				else
					contentsLevel.add( new Vec4d( top.get(i).getByInd(0), top.get(i).getByInd(1), ratio - 0.5, 1.0d ) );
			}

			for (int i = 0; i < contentsLevel.size() - 1; ++i) {

				// Contents sides
				if( ratio > 0.0 ) {
					if( ratio < 0.5 ) {
						ArrayList<Vec4d> rectangle = new ArrayList<>();
						rectangle.add( bottom.get(i) );
						rectangle.add( bottom.get(i+1) );
						rectangle.add( contentsLevel.get(i+1) );
						rectangle.add( contentsLevel.get(i) );
						cachedProxies.add(new PolygonProxy(rectangle,
								trans, scale, contentsColor, false, 0, vi, pickingID));
					}
					else {
						ArrayList<Vec4d> rectangle = new ArrayList<>();
						rectangle.add( bottom.get(i) );
						rectangle.add( bottom.get(i+1) );
						rectangle.add( middle.get(i+1) );
						rectangle.add( middle.get(i) );
						cachedProxies.add(new PolygonProxy(rectangle,
								trans, scale, contentsColor, false, 0, vi, pickingID));

						rectangle = new ArrayList<>();
						rectangle.add( middle.get(i) );
						rectangle.add( middle.get(i+1) );
						rectangle.add( contentsLevel.get(i+1) );
						rectangle.add( contentsLevel.get(i) );
						cachedProxies.add(new PolygonProxy(rectangle,
								trans, scale, contentsColor, false, 0, vi, pickingID));
					}
				}

				// Empty sides
				if( ratio < 1.0 ) {
					if( ratio < 0.5 ) {
						ArrayList<Vec4d> rectangle = new ArrayList<>();
						rectangle.add( contentsLevel.get(i) );
						rectangle.add( contentsLevel.get(i+1) );
						rectangle.add( middle.get(i+1) );
						rectangle.add( middle.get(i) );
						cachedProxies.add(new PolygonProxy(rectangle,
								trans, scale, backgroundColor, false, 0, vi, pickingID));

						rectangle = new ArrayList<>();
						rectangle = new ArrayList<>();
						rectangle.add( middle.get(i) );
						rectangle.add( middle.get(i+1) );
						rectangle.add( top.get(i+1) );
						rectangle.add( top.get(i) );
						cachedProxies.add(new PolygonProxy(rectangle,
								trans, scale, backgroundColor, false, 0, vi, pickingID));
					}
					else {
						ArrayList<Vec4d> rectangle = new ArrayList<>();
						rectangle.add( contentsLevel.get(i) );
						rectangle.add( contentsLevel.get(i+1) );
						rectangle.add( top.get(i+1) );
						rectangle.add( top.get(i) );
						cachedProxies.add(new PolygonProxy(rectangle,
								trans, scale, backgroundColor, false, 0, vi, pickingID));
					}
				}
			}

			// Add top surface
			if( ratio < 1.0 )
				cachedProxies.add(new PolygonProxy(top,
						trans, scale, backgroundColor, false, 0, vi, pickingID));
			else
				cachedProxies.add(new PolygonProxy(top,
						trans, scale, contentsColor, false, 0, vi, pickingID));

			// Add top outline
			cachedProxies.add(new PolygonProxy(top,
					trans, scale, ColourInput.BLACK, true, 1.5, vi, pickingID));

			// Add bottom outline
			cachedProxies.add(new PolygonProxy(bottom,
					trans, scale, ColourInput.BLACK, true, 1.5, vi, pickingID));

			// Add contents level outline
			cachedProxies.add(new PolygonProxy(contentsLevel,
					trans, scale, ColourInput.BLACK, true, 1.5, vi, pickingID));

			out.addAll(cachedProxies);
		}
	}

}
