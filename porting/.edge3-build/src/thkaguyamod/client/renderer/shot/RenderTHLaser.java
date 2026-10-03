package thkaguyamod.client.renderer.shot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import thkaguyamod.client.renderer.shot.DanmakuRenderTypes;
import thkaguyamod.entity.shot.EntityTHLaser;
import thkaguyamod.entity.shot.EntityTHSetLaser;
import thkaguyamod.entity.shot.EntityTHShot;

/**
 * レーザー（EntityTHLaser）の弾幕レンダラ。1.7.10 の
 * {@code thKaguyaMod.client.render.shot.RenderTHLaser} の忠実移植。
 *
 * <p>通常レーザーの円柱掃引、スピア・ザ・グングニル / レーヴァテインのハードコード頂点表、
 * マスタースパークのレインボー、母子レーザーを含む。頂点演算は 1.7.10 のまま。</p>
 *
 * <p>深度テストの切替（1.7.10 の {@code glDepthFunc(GL_ALWAYS)} → {@code GL_LEQUAL}）は、
 * RenderType を {@code noDepth} 版（{@link net.minecraft.client.renderer.RenderStateShard#NO_DEPTH_TEST}）に
 * 切り替えることで再現している。マスタースパークの 1 パス目のみ noDepth を使う。</p>
 *
 * <p>TODO K3-review: 1.7.10 は {@code glDepthMask} を描画途中で切り替えていたが、1.21.1 の
 * バッチ式では途中変更できないため、RenderType の writeMask（COLOR_WRITE）で近似している。
 * 統合時に 1.7.10 の各分岐と突き合わせて確認すること。また頂点表の {@code time} 引数は
 * 1.7.10 の {@code ticksExisted}（= {@code tickCount}）を想定している。</p>
 */
public class RenderTHLaser extends EntityRenderer<EntityTHLaser>
{
	private static final ResourceLocation laserTexture = DanmakuRenderTypes.texture("Laser");
	private static final ResourceLocation lightTexture = DanmakuRenderTypes.texture("MusouFuuin");

	/**
	 * # perf: B6 — 描画コンテキストの再利用用フィールド。
	 * 根因: 以前は {@code render()} ごとに {@code new Ctx(...)} していた。レンダースレッド単一なので使い回せる。
	 */
	private final Ctx ctx = new Ctx();

	/**
	 * # perf: B2 — スピア・ザ・グングニルの頂点表 / 面表。
	 * 根因: 以前は {@code renderSpearTheGungnir} 呼び出しごとに {@code double[28][3]} + {@code int[24][4]} を
	 * 新規作成していた（両面レンダリングで每帧 2 回呼ばれるため 4 個の配列）。純粋な定数なので static final 化する。
	 */
	private static final double[][] GUNGNIR_VERTEX = {
			 {0.000000,0.681531,-5.956767}
			 ,{0.000000,0.999996,6.000000}
			 ,{-0.681527,0.000004,-5.956767}
			 ,{-1.000000,-0.000004,6.000000}
			 ,{-0.481912,0.481916,-5.956767}
			 ,{-0.707107,0.707103,6.000000}
			 ,{-0.000000,1.957804,8.160793}
			 ,{-1.733818,-0.000005,4.933474}
			 ,{-1.461115,0.707101,6.843752}
			 ,{-2.247491,-0.00000,6.180964}
			 ,{-3.164763,-0.00000,6.988164}
			 ,{-4.155417,-0.00000,6.107583}
			 ,{-5.292834,-0.00000,3.575912}
			 ,{-5.476289,-0.00000,7.743996}
			 ,{-4.321013,-0.00000,11.094296}
			 ,{-0.787193,-0.00000,13.423745}
			 ,{0.005330,-0.00000,15.669228}
			 ,{-3.180439,1.438709,7.695314}
			 ,{-4.787621,0.707101,6.067568}
			 ,{-4.588031,0.707101,7.597761}
			 ,{0.00,1.996092,10.168588}
			 ,{-0.00,1.370200,12.822122}
			 ,{-3.372246,1.295732,10.534312}
			 ,{-1.406365,1.812266,10.156220}
			 ,{-1.433740,1.775428,8.389470}
			 ,{-0.000000,0.000006,-9.680047}
			 ,{-0.000000,1.076022,0.000000}
			 ,{-1.076022,0.000000,0.000000}
			 ,{-0.760863,0.760862,0.000000}
	};

	private static final int[][] GUNGNIR_ORDER = {
		{29,6,2,27}
		,{28,4,6,29}
		,{23,15,14,20}
		,{13,19,12,12}
		,{14,20,19,13}
		,{8,9,4,4}
		,{9,8,10,10}
		,{9,10,11,18}
		,{18,11,12,19}
		,{20,18,23,23}
		,{19,20,18,18}
		,{17,22,16,16}
		,{22,16,15,23}
		,{4,9,6,6}
		,{9,18,25,25}
		,{23,18,25,24}
		,{24,25,7,21}
		,{22,23,24,21}
		,{6,25,7,2}
		,{9,25,6,6}
		,{5,1,26,26}
		,{3,5,26,26}
		,{5,29,27,1}
		,{3,28,29,5}
	};

	/**
	 * # perf: B2 — レーヴァテインレーザーの頂点表（{@code Math.sin(time...)} 成分を除いた定数部）。
	 * 元表の 0-based 行 {@link #LAEVATEINN_TIME_X_ROW} の X 成分だけが time 依存なので、
	 * 描画時に配列を複製して当該行へ sin 加算する（{@code renderLaevateinn} 参照）。
	 */
	private static final double[][] LAEVATEINN_VERTEX = {
			{-0.012712,0.860909,19.683685},
			{0.010621,0.799994,3.958289},
			{-0.411288,0.576921,19.683094},
			{-0.555063,0.565679,3.957450},
			{-0.576384,0.000006,19.682850},
			{-0.789378,-0.000006,3.957103},
			{0.944878,0.000006,19.685106},
			{0.810621,-0.000006,3.959476},
			{0.385864,0.576921,19.684277},
			{0.576306,0.565679,3.959129},
			{0.000000,1.097230,11.116394},
			{-0.565685,0.803469,11.115555},
			{-0.978337,-0.000000,11.115208},
			{0.799999,0.000000,11.117582},
			{0.565685,0.922361,11.117234},
			{0.010666,0.838774,3.928527},
			{-2.699365,0.740431,3.924505},
			{-3.821897,-0.000008,3.922842},
			{3.843230,-0.000008,3.934216},
			{2.720698,0.819214,3.932549},
			{0.011292,0.878166,3.507193},
			{-2.698740,0.819214,3.503171},
			{-3.821272,-0.000009,3.501507},
			{3.843855,-0.000009,3.512882},
			{2.721323,0.819214,3.511215},
			{0.011649,0.801885,3.266243},
			{-0.603876,0.559341,3.293626},
			{-0.844117,-0.001264,3.318336},
			{0.867178,-0.000875,3.320815},
			{0.627090,0.559341,3.295454},
			{0.015762,0.620065,0.494560},
			{-0.422696,0.438449,0.493910},
			{-0.604311,-0.000009,0.493641},
			{0.635834,-0.000009,0.495482},
			{0.454219,0.438449,0.495212},
			{0.017635,-0.000009,-0.767668},
			{-0.017040,0.277390,22.599312},
			{-0.213180,0.196148,22.599022},
			{-0.294422,0.000008,22.598900},
			{0.260342,0.000008,22.599724},
			{0.179100,0.196148,22.599602},
			{-0.019185,0.000008,24.045269},
			{-0.488487,0.482134,15.399324},
			{-1.282651,0.000003,15.399029},
			{1.620884,0.000003,15.402454},
			{0.869693,0.482134,15.401340},
			{-0.006356,0.681839,15.400040},
			{0.005311,0.799997,7.537342},
			{-0.560374,0.565682,7.536503},
			{-1.151364,-0.000003,7.536155},
			{2.026458,-0.000003,7.540341},
			{1.161873,0.565682,7.539058}
	};

	/** time 依存（X 成分に {@code sin(time * speed / 180F * PI) * 0.5}）の行番号（0-based）。 */
	private static final int[] LAEVATEINN_TIME_X_ROW = { 6, 8, 39, 44, 50 };
	/** 上記各行の {@code sin} 引数に掛かる速度（元コードの 60 / 86 / 50 / 43 / 80）。 */
	private static final int[] LAEVATEINN_TIME_X_SPEED = { 60, 86, 50, 43, 80 };

	private static final int[][] LAEVATEINN_ORDER = {
			{48,2,4,49},
			{49,4,6,50},
			{2,10,20,16},
			{52,10,2,48},
			{51,8,10,52},
			{9,1,37,41},
			{47,11,12,43},
			{43,12,13,44},
			{46,15,11,47},
			{45,14,15,46},
			{16,20,25,21},
			{10,8,19,20},
			{6,4,17,18},
			{4,2,16,17},
			{20,19,24,25},
			{18,17,22,23},
			{17,16,21,22},
			{21,25,30,26},
			{25,24,29,30},
			{23,22,27,28},
			{22,21,26,27},
			{26,30,35,31},
			{30,29,34,35},
			{28,27,32,33},
			{27,26,31,32},
			{31,35,36,36},
			{35,34,36,36},
			{33,32,36,36},
			{32,31,36,36},
			{7,9,41,40},
			{3,5,39,38},
			{1,3,38,37},
			{41,37,42,42},
			{40,41,42,42},
			{38,39,42,42},
			{37,38,42,42},
			{1,47,43,3},
			{3,43,44,5},
			{9,46,47,1},
			{7,45,46,9},
			{11,48,49,12},
			{12,49,50,13},
			{15,52,48,11},
			{14,51,52,15}
	};

	/** 1.7.10 の色パレット（数値はそのまま）。 */
	protected float colorR[] = { 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F, 255F/255F};
	protected float colorG[] = {   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 224F/255F, 128F/255F, 255F/255F};
	protected float colorB[] = {   0F/255F, 224F/255F,   0F/255F,   0F/255F, 224F/255F, 224F/255F,   0F/255F, 255F/255F};

	public RenderTHLaser(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityTHLaser entity)
	{
		if(entity.getAnimationCount() < 0 && entity instanceof EntityTHSetLaser == false)
		{
			return lightTexture;
		}
		return laserTexture;
	}

	@Override
	public void render(EntityTHLaser entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight)
	{
		ResourceLocation texture = this.getTextureLocation(entity);
		poseStack.pushPose();
		// # perf: B6 — new Ctx せずフィールドを使い回す（レンダースレッド単一なので安全）
		this.ctx.set(poseStack, bufferSource, texture, this.entityRenderDispatcher);
		Ctx ctx = this.ctx;
		this.renderTHLaser(ctx, entity, entityYaw, entity.getXRot());// 1.7.10 の rotationPitch
		poseStack.popPose();
	}

	public void renderTHLaser(Ctx ctx, EntityTHLaser thLaser, float yaw, float pitch)
	{
		int color = thLaser.getShotId();

		float width = 1.0F;
		if(thLaser.getAnimationCount() < 0 && thLaser instanceof EntityTHSetLaser)
		{
			width = 0.1F;
		}

		ctx.pose.scale(1.0F, 1.0F, 1.0F);

		if(thLaser.getAnimationCount() < 0 && thLaser instanceof EntityTHSetLaser == false)
		{
			// --- 遅延期は光の予告のみ ---
			int delayCount = -thLaser.getAnimationCount();
			if(delayCount > 10)
			{
				delayCount = 10;
			}
			float size2 = delayCount * thLaser.getShotSize() * 2F;
			if(size2 > 1.0F)
			{
				size2 = 1.0F;
			}
			ctx.pose.pushPose();
			// 1.7.10 は renderManager.playerView によるビルボード（常に正面）
			ctx.pose.mulPose(ctx.entityRenderDispatcher.cameraOrientation());
			renderLightEffect(ctx, color % 8, size2, thLaser);
			ctx.pose.popPose();
		}
		else
		{
			ctx.pose.pushPose();
			ctx.pose.mulPose(Axis.YP.rotationDegrees(yaw));
			ctx.pose.mulPose(Axis.XP.rotationDegrees(-pitch));
			double centerZ1 = thLaser.getLaserLength() * 1.2D / 2.0D;
			double centerZ2 = thLaser.getLaserLength() / 2.0D;

			if(color >= 24)//マスタースパークレーザーなら
			{
				ctx.pose.scale(width, width, 1.0F);
				float ticks = (float)thLaser.getAnimationCount() * 10.0F;
				ticks = Math.abs(ticks);
				// 1.7.10: glDepthFunc(GL_ALWAYS) → noDepthTest=true（深度書き込みは true のまま）
				renderMasterSparkLaser(ctx, thLaser.getLaserLength(), thLaser.getShotSize(), 1.0F, false, ticks,
						DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, true);
				// 1.7.10: glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR) / glDepthFunc(GL_LEQUAL) に復帰
				renderMasterSparkLaser(ctx, thLaser.getLaserLength(), thLaser.getShotSize() * 1.2F, 0.6F, true, ticks,
						DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false);
			}
			else if(color >= 16)//レーヴァテインレーザーなら
			{
				ctx.pose.mulPose(Axis.ZP.rotationDegrees(thLaser.getAngleZ()));
				ctx.pose.scale(1.0F, 1.0F, 1.0F * (float)thLaser.getLaserLength() / 20.8F);
				// 1 パス目（glDepthMask(true) のまま・(SRC_ALPHA, ONE)）
				renderLaevateinn(ctx, ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA), ctx.pose.last(),
						thLaser.getLaserLength(), thLaser.getShotSize(),  1.6D, 7, 1.0F, thLaser.tickCount);

				// 1.7.10: glDepthMask(false) → glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)
				ctx.pose.scale(1.2F, 1.2F, 1.2F);
				renderLaevateinn(ctx, ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true), ctx.pose.last(),
						thLaser.getLaserLength() * 1.2D, thLaser.getShotSize() * 1.2F, 1.0D, color, 0.6F,  thLaser.tickCount);
			}
			else if(color >= 8)//グングニルレーザーなら
			{
				ctx.pose.mulPose(Axis.ZP.rotationDegrees((float)thLaser.tickCount * 30F));
				ctx.pose.scale(0.3F, 0.3F, 0.5F  * (float)thLaser.getLaserLength() / 10F);
				renderSpearTheGungnir(ctx, ctx.buf(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA), ctx.pose.last(),
						thLaser.getLaserLength(), thLaser.getShotSize(), centerZ1 - centerZ2, 7, 1.0F);

				// 1.7.10: glDepthMask(false) → glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)
				ctx.pose.scale(1.2F, 1.2F, 1.2F);
				renderSpearTheGungnir(ctx, ctx.buf(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR, false, true), ctx.pose.last(),
						thLaser.getLaserLength() * 1.2D, thLaser.getShotSize() * 1.2F, -1.0D, color, 0.6F);
			}
			else if(thLaser.isMotherAndChild() != (byte)0)
			{
				// 1.7.10 の getDistance(getMotherPosX(), ...) は親レーザー位置までの距離
				double toMotherLength = Math.sqrt(thLaser.distanceToSqr(thLaser.getMotherPosX(), thLaser.getMotherPosY(), thLaser.getMotherPosZ()));
				// 1.7.10 はこの分岐で GL11.glEnable(GL11.GL_CULL_FACE)（RenderTHLaser.java:145）→ CULL 版
				// # perf: A5
				renderLaser(ctx, ctx.bufCulled(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA), ctx.pose.last(),
						toMotherLength, thLaser.getShotSize(), 0.0D, color, 1.0F);
			}
			else
			{
				ctx.pose.scale(width, width, 1.0F);
				// 1.7.10 はこの分岐で GL11.glEnable(GL11.GL_CULL_FACE)（RenderTHLaser.java:155）→ CULL 版
				// # perf: A5 — renderLaser は 13×9=117 quad/チャネル、カリングでラスタライズ量が半減
				renderLaser(ctx, ctx.bufCulled(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA), ctx.pose.last(),
						thLaser.getLaserLength(), thLaser.getShotSize(), centerZ1 - centerZ2, 7, 1.0F);
				// 1.7.10: glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR)（GL_CULL_FACE は有効のまま）
				renderLaser(ctx, ctx.bufCulled(DanmakuRenderTypes.MODE_ADD_ONE_MINUS_SRC_COLOR), ctx.pose.last(),
						thLaser.getLaserLength() * 1.2D, thLaser.getShotSize() * 1.2F, 0.0D, color, 0.6F);
			}
			ctx.pose.popPose();
		}
	}

	//レーザーの描画をする（円柱掃引）
	protected void renderLaser(Ctx ctx, VertexConsumer b, PoseStack.Pose pose, double length, float width, double zPos, int color, float alpha)
	{
		float maxWidth = (float)width;//最大の太さをmaxWidthとして保存

		int zAngleDivNum = 8;//Z軸回転の分割数
		double angleZ = 0F;//Z軸回転変数
		double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;//Z軸回転の変化量

		int zDivNum = 13;//レーザーの奥方向への分割数。必ず奇数
		double zLength = length;//レーザーの長さ（Z方向の長さ、奥行き）
		double zDivLength = zLength / (double)(zDivNum - 1);//Z方向へ分割したときの1分割分の長さ
		double zPosOld = zPos;//ひとつ前の描画位置
		float xPos = 0F;
		float yPos = 0F;
		float xPos2 = 0F;
		float yPos2 = 0F;
		float xPosOld = xPos;
		float yPosOld = yPos;
		float xPos2Old = xPos2;
		float yPos2Old = yPos2;
		float angle = -(float)Math.PI / 2.0F;
		float angleSpan = (float)Math.PI / (float)(zDivNum);
		angle += angleSpan;
		float widthOld = 0.0F;

		for(int j = 0; j < zDivNum; j++)
		{
			zPos += zDivLength;//奥行きを１段階増やす
			xPos = width;
			yPos = 0F;
			xPosOld = (float)Math.cos(angleZ) * width;
			yPosOld = (float)Math.sin(angleZ) * width;
			xPos2Old = (float)Math.cos(angleZ) * widthOld;
			yPos2Old = (float)Math.sin(angleZ) * widthOld;
			angleZ = angleSpanZ;

			for(int i = 0; i <= zAngleDivNum; i++)
			{
				xPos = (float)Math.cos(angleZ) * width;
				yPos = (float)Math.sin(angleZ) * width;
				xPos2 = (float)Math.cos(angleZ) * widthOld;
				yPos2 = (float)Math.sin(angleZ) * widthOld;

				quad(b, pose,
						xPosOld, yPosOld, (float)zPos, 0.0F, 0.0F,
						xPos2Old, yPos2Old, (float)zPosOld, 0.0F, 1.0F,
						xPos2, yPos2, (float)zPosOld, 1.0F, 1.0F,
						xPos, yPos, (float)zPos, 1.0F, 0.0F,
						colorR[color], colorG[color], colorB[color], alpha);

				xPosOld = xPos;
				yPosOld = yPos;
				xPos2Old = xPos2;
				yPos2Old = yPos2;
				angleZ += angleSpanZ;
			}
			zPosOld = zPos;//古い奥行きを今の奥行きに更新
			angle += angleSpan;//レーザーの描く半円の角度を更新
			//widthOld = width;
		}
	}

	//スピア・ザ・グングニルの描画をする
	protected void renderSpearTheGungnir(Ctx ctx, VertexConsumer b, PoseStack.Pose pose, double length, float width, double zPos, int color, float alpha)
	{
		int c = color % 8;
		// # perf: B2 — 頂点表/面表は static final（GUNGNIR_VERTEX / GUNGNIR_ORDER）へ移動
		double[][] vertex = GUNGNIR_VERTEX;
		int[][] order = GUNGNIR_ORDER;

		for(int i = 0; i < 24; i++)
		{
			quad(b, pose,
					(float)vertex[order[i][0] - 1][0],  (float)vertex[order[i][0] - 1][1], (float)vertex[order[i][0] - 1][2] + (float)zPos, 0.0F, 0.0F,
					(float)vertex[order[i][1] - 1][0],  (float)vertex[order[i][1] - 1][1], (float)vertex[order[i][1] - 1][2] + (float)zPos, 0.0F, 1.0F,
					(float)vertex[order[i][2] - 1][0],  (float)vertex[order[i][2] - 1][1], (float)vertex[order[i][2] - 1][2] + (float)zPos, 1.0F, 1.0F,
					(float)vertex[order[i][3] - 1][0],  (float)vertex[order[i][3] - 1][1], (float)vertex[order[i][3] - 1][2] + (float)zPos, 1.0F, 0.0F,
					colorR[c], colorG[c], colorB[c], alpha);

			quad(b, pose,
					-(float)vertex[order[i][0] - 1][0],  (float)vertex[order[i][0] - 1][1], (float)vertex[order[i][0] - 1][2] + (float)zPos, 0.0F, 0.0F,
					-(float)vertex[order[i][1] - 1][0],  (float)vertex[order[i][1] - 1][1], (float)vertex[order[i][1] - 1][2] + (float)zPos, 0.0F, 1.0F,
					-(float)vertex[order[i][2] - 1][0],  (float)vertex[order[i][2] - 1][1], (float)vertex[order[i][2] - 1][2] + (float)zPos, 1.0F, 1.0F,
					-(float)vertex[order[i][3] - 1][0],  (float)vertex[order[i][3] - 1][1], (float)vertex[order[i][3] - 1][2] + (float)zPos, 1.0F, 0.0F,
					colorR[c], colorG[c], colorB[c], alpha);

			quad(b, pose,
					(float)vertex[order[i][0] - 1][0],  -(float)vertex[order[i][0] - 1][1], (float)vertex[order[i][0] - 1][2] + (float)zPos, 0.0F, 0.0F,
					(float)vertex[order[i][1] - 1][0],  -(float)vertex[order[i][1] - 1][1], (float)vertex[order[i][1] - 1][2] + (float)zPos, 0.0F, 1.0F,
					(float)vertex[order[i][2] - 1][0],  -(float)vertex[order[i][2] - 1][1], (float)vertex[order[i][2] - 1][2] + (float)zPos, 1.0F, 1.0F,
					(float)vertex[order[i][3] - 1][0],  -(float)vertex[order[i][3] - 1][1], (float)vertex[order[i][3] - 1][2] + (float)zPos, 1.0F, 0.0F,
					colorR[c], colorG[c], colorB[c], alpha);

			quad(b, pose,
					-(float)vertex[order[i][0] - 1][0],  -(float)vertex[order[i][0] - 1][1], (float)vertex[order[i][0] - 1][2] + (float)zPos, 0.0F, 0.0F,
					-(float)vertex[order[i][1] - 1][0],  -(float)vertex[order[i][1] - 1][1], (float)vertex[order[i][1] - 1][2] + (float)zPos, 0.0F, 1.0F,
					-(float)vertex[order[i][2] - 1][0],  -(float)vertex[order[i][2] - 1][1], (float)vertex[order[i][2] - 1][2] + (float)zPos, 1.0F, 1.0F,
					-(float)vertex[order[i][3] - 1][0],  -(float)vertex[order[i][3] - 1][1], (float)vertex[order[i][3] - 1][2] + (float)zPos, 1.0F, 0.0F,
					colorR[c], colorG[c], colorB[c], alpha);
		}
	}

	//レーヴァテインの描画をする
	protected void renderLaevateinn(Ctx ctx, VertexConsumer b, PoseStack.Pose pose, double length, float width, double zPos, int color, float alpha, int time)
	{
		int c = color % 8;
		// # perf: B2 — 頂点表/面表は static final（LAEVATEINN_VERTEX / LAEVATEINN_ORDER）へ移動。
		// 根因: 以前は呼び出しごとに double[52][3] + int[44][4] を作っていた（両面で每帧 2 回 → 4 個）。
		// 元表には Math.sin(time * N / 180F * PI) * 0.5 を含む 5 成分があり、それらは time に依存するため
		// 定数化できない。該当 5 行だけ深いコピーを取り、X 成分に sin を加算する（元と同値）。
		double[][] vertex = LAEVATEINN_VERTEX.clone();
		int[][] order = LAEVATEINN_ORDER;
		for(int i = 0; i < LAEVATEINN_TIME_X_ROW.length; i++)
		{
			int row = LAEVATEINN_TIME_X_ROW[i];
			vertex[row] = LAEVATEINN_VERTEX[row].clone();
			vertex[row][0] += Math.sin(time * LAEVATEINN_TIME_X_SPEED[i] / 180F * 3.141593F) * 0.5;
		}

		for(int i = 0; i < order.length; i++)
		{
			quad(b, pose,
					(float)vertex[order[i][0] - 1][0],  (float)vertex[order[i][0] - 1][1], (float)vertex[order[i][0] - 1][2] + (float)zPos, 0.0F, 0.0F,
					(float)vertex[order[i][1] - 1][0],  (float)vertex[order[i][1] - 1][1], (float)vertex[order[i][1] - 1][2] + (float)zPos, 0.0F, 1.0F,
					(float)vertex[order[i][2] - 1][0],  (float)vertex[order[i][2] - 1][1], (float)vertex[order[i][2] - 1][2] + (float)zPos, 1.0F, 1.0F,
					(float)vertex[order[i][3] - 1][0],  (float)vertex[order[i][3] - 1][1], (float)vertex[order[i][3] - 1][2] + (float)zPos, 1.0F, 0.0F,
					colorR[c], colorG[c], colorB[c], alpha);

			quad(b, pose,
					(float)vertex[order[i][0] - 1][0],  -(float)vertex[order[i][0] - 1][1], (float)vertex[order[i][0] - 1][2] + (float)zPos, 0.0F, 0.0F,
					(float)vertex[order[i][1] - 1][0],  -(float)vertex[order[i][1] - 1][1], (float)vertex[order[i][1] - 1][2] + (float)zPos, 0.0F, 1.0F,
					(float)vertex[order[i][2] - 1][0],  -(float)vertex[order[i][2] - 1][1], (float)vertex[order[i][2] - 1][2] + (float)zPos, 1.0F, 1.0F,
					(float)vertex[order[i][3] - 1][0],  -(float)vertex[order[i][3] - 1][1], (float)vertex[order[i][3] - 1][2] + (float)zPos, 1.0F, 0.0F,
					colorR[c], colorG[c], colorB[c], alpha);
		}
	}

	//マスタースパークのレーザー
	protected void renderMasterSparkLaser(Ctx ctx, double length, float width, float alpha, boolean rainbowFlag, float ticks, String mode, boolean noDepth)
	{
		float localColorR[] = { 255F, 255F, 224F,   0F,   0F,   0F, 224F, 255F};
		float localColorG[] = {   0F, 165F, 224F, 255F,   0F, 255F,   0F, 255F};
		float localColorB[] = {   0F,   0F,   0F,   0F, 255F, 255F, 224F, 255F};

		VertexConsumer b = ctx.buf(mode, noDepth, false);
		PoseStack.Pose pose = ctx.pose.last();

		float maxWidth = (float)width * 1.6F;//最大の太さをmaxWidthとして保存

		int zAngleDivNum = 64;//Z軸回転の分割数
		if(!rainbowFlag)
		{
			zAngleDivNum = 16;
		}
		double angleZ;//Z軸回転変数
		double angleSpanZ = Math.PI * 2.0D / (double)zAngleDivNum;//Z軸回転の変化量

		int zDivNum = 13;//レーザーの奥方向への分割数。必ず奇数
		double zLength = length;//レーザーの長さ（Z方向の長さ、奥行き）
		double zDivLength = zLength / (double)(zDivNum - 1);//Z方向へ分割したときの1分割分の長さ
		double zPos = 0.0D;
		double zPosOld = zPos;//ひとつ前の描画位置
		float xPos = 0F;
		float yPos = 0F;
		float xPos2 = 0F;
		float yPos2 = 0F;
		float xPosOld = xPos;
		float yPosOld = yPos;
		float xPos2Old = xPos2;
		float yPos2Old = yPos2;
		float angle = 0F;
		float angleSpan = (float)Math.PI / (float)(zDivNum - 1);
		width = (float)Math.sin(angle) * maxWidth;
		float widthOld = width;

		float color;
		float colorAngle = 0F;
		float colorRev = 3.141593F * 5.6125F / 180F;

		float width2 = width;
		float widthOld2 = widthOld;

		while(zPos < zLength)
		{
			zPos += zDivLength;//奥行きを１段階増やす
			widthOld = width;
			angle += angleSpan;//レーザーの描く半円の角度を更新
			if(width < maxWidth)
			{
				width = (float)Math.sin(angle) * maxWidth;
			}
			else
			{
				width = maxWidth;
			}

			widthOld2 = width2;
			width2 = width * ((float)Math.sin(angle + (float)ticks / 40F) * 0.1F + 1.0F);
			xPos = 0F;
			yPos = (float)width;
			angleZ = angleSpanZ;

			for(int i = 0; i <= zAngleDivNum; i++)
			{
				xPos = (float)Math.cos(angleZ) * width2;
				yPos = (float)Math.sin(angleZ) * width2;
				xPos2 = (float)Math.cos(angleZ) * widthOld2;
				yPos2 = (float)Math.sin(angleZ) * widthOld2;

				float r, g, bl, a;
				if(rainbowFlag)
				{
					color = ((float)colorAngle + (float)ticks) * colorRev;
					colorAngle++;
					// 1.7.10: setColorRGBA_F(sin(color), cos(color), -sin(color)*2F, 0.60F)
					//   注意: 1.7.10 の setColorRGBA_F は値をそのまま使う。1.21.1 の setColor(f,f,f,f) は
					//   *255 して int 化するため、原値（sin は負もとる）はそのままではクランプされる。
					//   TODO K3-review: 1.7.10 の負の成分は固定機能では 0 にクランプされていた可能性が高い。
					//   ここでは 0..1 にクランプして渡す（見た目は概ね等価だが要確認）。
					r = Mth.clamp(Mth.sin(color), 0.0F, 1.0F);
					g = Mth.clamp(Mth.cos(color), 0.0F, 1.0F);
					bl = Mth.clamp(-Mth.sin(color) * 2F, 0.0F, 1.0F);
					a = 0.60F;
				}
				else
				{
					r = 1.0F;
					g = 1.0F;
					bl = 1.0F;
					a = alpha;
				}

				quad(b, pose,
						xPosOld, yPosOld, (float)zPos, 0.0F, 0.0F,
						xPos, yPos, (float)zPos, 1.0F, 0.0F,
						xPos2, yPos2, (float)zPosOld, 1.0F, 1.0F,
						xPos2Old, yPos2Old, (float)zPosOld, 0.0F, 1.0F,
						r, g, bl, a);

				xPosOld = xPos;
				yPosOld = yPos;
				xPos2Old = xPos2;
				yPos2Old = yPos2;
				angleZ += angleSpanZ;
			}
			ticks += 1.0F;
			zPosOld = zPos;//古い奥行きを今の奥行きに更新
		}
	}

	//光のエフェクト
	public void renderLightEffect(Ctx ctx, int color, float size, EntityTHShot thShot)
	{
		int c = color % 8;
		// 遅延期は getAnimationCount() が負になり得るため、負値でも 0/1 になる & 1 を使う
		// （1.7.10 は % 2 で -1 になり UV が負になっていた。I7 の RenderTHShot.renderLightEffect と同じ扱い）。
		int count = thShot.getAnimationCount() & 1;
		float f3 = (float)(count * 32 +  0) / 64F;
		float f4 = (float)(count * 32 + 32) / 64F;
		float f5 = 0F;
		float f6 = 1F;
		float f7 = 1.0F;
		float f8 = 0.5F;
		float f9 = 0.25F;
		// 1.7.10: glDepthMask(false) / GL_CULL_FACE は有効のまま（RenderTHLaser.java:65-82 は
		// GL_CULL_FACE に触れない）→ CULL 版。# perf: A5
		VertexConsumer b = ctx.bufCulled(DanmakuRenderTypes.MODE_ADD_SRC_ALPHA, false, true);
		ctx.pose.pushPose();
		ctx.pose.scale(size, size, size);
		quad(b, ctx.pose.last(),
				0.0F - f8, 0.0F - f9, 0.0F, f3, f6,
				f7 - f8, 0.0F - f9, 0.0F, f4, f6,
				f7 - f8, 1.0F - f9, 0.0F, f4, f5,
				0.0F - f8, 1.0F - f9, 0.0F, f3, f5,
				colorR[c], colorG[c], colorB[c], 0.5F);
		ctx.pose.popPose();
	}

	// ====================================================================
	// 補助（RenderTHShot と同一。取り込み時に共有ユーティリティ化してよい）
	// ====================================================================

	/** 4 頂点（QUADS）を 1 枚書き込む。 */
	private static void quad(VertexConsumer b, PoseStack.Pose pose,
			float x0, float y0, float z0, float u0, float v0,
			float x1, float y1, float z1, float u1, float v1,
			float x2, float y2, float z2, float u2, float v2,
			float x3, float y3, float z3, float u3, float v3,
			float r, float g, float bl, float a)
	{
		b.addVertex(pose, x0, y0, z0).setColor(r, g, bl, a).setUv(u0, v0);
		b.addVertex(pose, x1, y1, z1).setColor(r, g, bl, a).setUv(u1, v1);
		b.addVertex(pose, x2, y2, z2).setColor(r, g, bl, a).setUv(u2, v2);
		b.addVertex(pose, x3, y3, z3).setColor(r, g, bl, a).setUv(u3, v3);
	}

	/**
	 * 描画コンテキスト（poseStack / bufferSource / 現在のレーザーテクスチャ）。
	 *
	 * <p># perf: B6 — 以前は {@code render()} ごとに {@code new Ctx(...)} していた。
	 * レンダースレッド単一スレッドであることを利用し、{@link RenderTHLaser} がフィールドを
	 * 使い回せるように可変にした（外部利用者向けに 4 引数コンストラクタはそのまま）。</p>
	 */
	public static final class Ctx
	{
		public PoseStack pose;
		public MultiBufferSource buffers;
		public ResourceLocation texture;
		public net.minecraft.client.renderer.entity.EntityRenderDispatcher entityRenderDispatcher;

		/** 再利用用（フィールドとして保持し {@link #set} で中身を差し替える）。 */
		public Ctx()
		{
		}

		public Ctx(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, net.minecraft.client.renderer.entity.EntityRenderDispatcher erd)
		{
			this.set(pose, buffers, texture, erd);
		}

		/** # perf: B6 — 既存インスタンスの中身を差し替える（new しない）。 */
		public void set(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, net.minecraft.client.renderer.entity.EntityRenderDispatcher erd)
		{
			this.pose = pose;
			this.buffers = buffers;
			this.texture = texture;
			this.entityRenderDispatcher = erd;
		}

		public VertexConsumer buf(String mode)
		{
			return buf(mode, false, false);
		}

		public VertexConsumer bufCulled(String mode)
		{
			return bufCulled(mode, false, false);
		}

		/**
		 * # perf: A5 — 背面カリング版（1.7.10 で GL_CULL_FACE が有効だったパス用）。
		 * @param noDepthTest  true で glDepthFunc(GL_ALWAYS) 相当
		 * @param noDepthWrite true で glDepthMask(false) 相当（深度を書かない）
		 */
		public VertexConsumer bufCulled(String mode, boolean noDepthTest, boolean noDepthWrite)
		{
			return this.buffers.getBuffer(DanmakuRenderTypes.getCulled(this.texture, mode, noDepthTest, noDepthWrite));
		}

		/**
		 * @param noDepthTest  true で glDepthFunc(GL_ALWAYS) 相当
		 * @param noDepthWrite true で glDepthMask(false) 相当（深度を書かない）
		 */
		public VertexConsumer buf(String mode, boolean noDepthTest, boolean noDepthWrite)
		{
			return this.buffers.getBuffer(DanmakuRenderTypes.get(this.texture, mode, noDepthTest, noDepthWrite));
		}
	}
}
