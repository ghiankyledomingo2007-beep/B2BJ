import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/** Native-pixel armor/core effects, with sound triggered by the real boss state. */
final class GuardianDraftFx {
    private static final Color INK=new Color(23,18,31),VIOLET=new Color(74,39,87),GOLD=new Color(255,208,138),CYAN=new Color(46,209,205),WHITE=new Color(212,255,243);
    private record Burst(double x,double y,double started){}
    private final ArrayList<Burst> bursts=new ArrayList<>();
    private final int[][][] motion,sweep,rush;
    private GuardianDraft previousBoss;
    private Guardian.State previous=Guardian.State.DORMANT;
    private double clock,stepDistance,lastX,lastY;
    private boolean fistOne,fistTwo,phaseBloom;
    GuardianDraftFx(BufferedImage motion,BufferedImage sweep,BufferedImage rush){this.motion=anchors(motion);this.sweep=anchors(sweep);this.rush=anchors(rush);}
    private static int[][][] anchors(BufferedImage image){
        if(image==null)return new int[0][][];
        int cell=image.getWidth()/8,rows=image.getHeight()/cell;int[][][] result=new int[rows][8][2];
        for(int row=0;row<rows;row++)for(int f=0;f<8;f++){long x=0,y=0;int count=0;
            for(int sy=0;sy<cell;sy++)for(int sx=0;sx<cell;sx++){int rgb=image.getRGB(f*cell+sx,row*cell+sy),r=rgb>>>16&255,g=rgb>>>8&255,b=rgb&255;if((rgb>>>24)>240&&g>120&&b>120&&r<g*.7){x+=sx;y+=sy;count++;}}
            result[row][f][0]=count>0?(int)(x/count):cell/2;result[row][f][1]=count>0?(int)(y/count):cell/2;
        }return result;
    }
    double[] core(GuardianDraft b){int[][][] points=b.actionFrame()>=0?(b.move()==GuardianDraft.Move.BACKHAND?sweep:rush):motion;int row=b.actionFrame()>=0?0:b.animation().ordinal(),frame=b.actionFrame()>=0?b.actionFrame():b.animationFrame();
        int ax=60,ay=68;if(row<points.length){ax=points[row][frame][0];ay=points[row][frame][1];}
        if(b.actionFrame()>=0&&b.directionX()<0)ax=128-ax;
        return new double[]{b.x()-128+ax*2,b.y()-176+ay*2};
    }
    boolean update(double dt,GuardianDraft b){
        boolean impact=false;
        clock+=dt;bursts.removeIf(f->clock-f.started()>1.3);
        if(previousBoss!=b){previousBoss=b;previous=Guardian.State.DORMANT;lastX=b.x();lastY=b.y();stepDistance=0;fistOne=fistTwo=phaseBloom=false;bursts.clear();}
        if(b.state()==Guardian.State.APPROACH){stepDistance+=Math.hypot(b.x()-lastX,b.y()-lastY);if(stepDistance>=80){stepDistance%=80;GameAudio.play(GameAudio.Cue.WARDEN_STEP);}}
        lastX=b.x();lastY=b.y();
        if(b.state()!=previous){
            switch(b.state()){
                case TELEGRAPH->{fistOne=fistTwo=false;GameAudio.play(b.move()==GuardianDraft.Move.CORE_BEAM?GameAudio.Cue.WARDEN_BEAM_CHARGE:GameAudio.Cue.WARDEN_WINDUP);}
                case SLAM->{impact=b.move()!=GuardianDraft.Move.DETACHED_FISTS;switch(b.move()){
                    case FIST->{GameAudio.play(GameAudio.Cue.WARDEN_SLAM);burst(b.targetX(),b.targetY());}
                    case BACKHAND->GameAudio.play(GameAudio.Cue.WARDEN_SWEEP);
                    case RUSH->GameAudio.play(GameAudio.Cue.WARDEN_RUSH);
                    case DETACHED_FISTS->GameAudio.play(GameAudio.Cue.WARDEN_FISTS_LAUNCH);
                    case CORE_BEAM->GameAudio.play(GameAudio.Cue.WARDEN_BEAM);
                }}
                case RECOVER->GameAudio.play(GameAudio.Cue.WARDEN_CORE_OPEN);
                case ENRAGE->{phaseBloom=false;GameAudio.play(GameAudio.Cue.WARDEN_STRAIN);}
                default->{}
            }
        }
        if(b.state()==Guardian.State.SLAM&&b.move()==GuardianDraft.Move.DETACHED_FISTS){
            if(b.stateSeconds()>=.2&&!fistOne){fistOne=true;impact=true;GameAudio.play(GameAudio.Cue.WARDEN_FISTS_IMPACT);burst(b.firstX(),b.firstY());}
            if(b.stateSeconds()>=.68&&!fistTwo){fistTwo=true;impact=true;GameAudio.play(GameAudio.Cue.WARDEN_FISTS_IMPACT);burst(b.secondX(),b.secondY());}
        }
        if(b.state()==Guardian.State.ENRAGE&&b.stateSeconds()>=1.2&&!phaseBloom){phaseBloom=true;impact=true;burst(b.x(),b.y());}
        previous=b.state();
        return impact;
    }
    private void burst(double x,double y){bursts.add(new Burst(x,y,clock));}
    private static double clamp(double v){return Math.max(0,Math.min(1,v));}
    private static void color(Graphics2D g,Color c,double alpha){g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(clamp(alpha)*255)));}
    private static void line(Graphics2D g,double x,double y,double ex,double ey,Color c,int width,double alpha){color(g,c,alpha);g.setStroke(new BasicStroke(width));g.drawLine((int)Math.round(x),(int)Math.round(y),(int)Math.round(ex),(int)Math.round(ey));}
    private static void shard(Graphics2D g,int x,int y,double alpha){color(g,INK,alpha);g.fillPolygon(new int[]{x-4,x,x+4,x+2,x-3},new int[]{y,y-4,y-1,y+3,y+2},5);color(g,VIOLET,alpha);g.fillPolygon(new int[]{x-2,x,x+3,x+1},new int[]{y,y-3,y,y+2},4);line(g,x-2,y-1,x,y-3,GOLD,1,alpha*.8);}
    void ground(Graphics2D source,GuardianDraft b,int cx,int cy,boolean reduced){var g=(Graphics2D)source.create();g.scale(2,2);
        for(Burst burst:bursts){double age=clock-burst.started(),fade=1-age/1.3;int x=(int)((burst.x()-cx)/2),y=(int)((burst.y()+Player.COLLISION_Y_OFFSET-cy)/2);
            for(int i=0;i<7;i++){double a=i*Math.PI*2/7,reach=(26+i%3*7)*clamp(age/.12),mx=x+Math.cos(a)*reach*.5,my=y+Math.sin(a)*reach*.25;
                line(g,x,y,mx,my,INK,3,fade);line(g,mx,my,mx+Math.cos(a+.25)*reach*.5,my+Math.sin(a+.25)*reach*.25,INK,3,fade);
                line(g,x,y,mx,my,CYAN,1,clamp(1-age/(b.enraged()?.9:.45)));}
            if(age<.2){int r=(int)(6+age*160);color(g,CYAN,(1-age/.2)*.6);g.setStroke(new BasicStroke(1));g.drawArc(x-r,y-r/3,r*2,r*2/3,10,115);g.drawArc(x-r,y-r/3,r*2,r*2/3,180,120);}
            if(!reduced)for(int i=0;i<7;i++){double a=i*2.4,dx=Math.cos(a)*age*60,dy=Math.sin(a)*age*28-Math.max(0,age*65-age*age*95);shard(g,x+(int)dx,y+(int)dy,fade);}
        }
        if(b.move()==GuardianDraft.Move.DETACHED_FISTS&&b.committed()&&(b.state()==Guardian.State.TELEGRAPH||b.state()==Guardian.State.SLAM)){
            for(int i=0;i<(b.state()==Guardian.State.SLAM?2:1);i++){double age=b.stateSeconds()-(i==0?.2:.68);if(b.state()==Guardian.State.SLAM&&age>0)continue;int x=(int)(((i==0?b.firstX():b.secondX())-cx)/2),y=(int)(((i==0?b.firstY():b.secondY())+Player.COLLISION_Y_OFFSET-cy)/2);
                color(g,INK,.65);g.fillOval(x-13,y-4,26,8);line(g,x-8,y,x-2,y-2,CYAN,1,.7);line(g,x+2,y-2,x+8,y,CYAN,1,.7);}
        }g.dispose();
    }
    void body(Graphics2D source,GuardianDraft b,int cx,int cy,boolean reduced){if(!b.alive())return;var g=(Graphics2D)source.create();g.scale(2,2);double[] point=core(b);double x=(point[0]-cx)/2,y=(point[1]-cy)/2;
        double power=b.coreExposed()?1:b.enraged()?.75:b.state()==Guardian.State.TELEGRAPH?.3:0;
        if(power>0){color(g,CYAN,power*.16);g.fillOval((int)x-17,(int)y-10,34,20);line(g,x-10,y-3,x,y+4,CYAN,2,power);line(g,x,y+4,x+10,y-3,CYAN,2,power);}
        if(b.enraged())for(int side:new int[]{-1,1}){line(g,x+side*14,y-8,x+side*24,y+2,CYAN,1,.7);line(g,x+side*24,y+2,x+side*19,y+13,CYAN,1,.7);for(int i=0;i<(reduced?1:3);i++)shard(g,(int)((b.x()-cx)/2)+side*(42+i*6),(int)y-10+i*17+(int)(Math.sin(clock*2+i)*3),.9);}
        if(b.state()==Guardian.State.ENRAGE){color(g,CYAN,.18+clamp(b.stateSeconds()/1.4)*.25);g.fillOval((int)x-20,(int)y-12,40,24);}
        g.dispose();
    }
    private static void fist(Graphics2D g,int x,int y){color(g,INK,1);g.fillPolygon(new int[]{x-13,x-5,x+3,x+12,x+13,x+5,x-11},new int[]{y-9,y-13,y-11,y-5,y+8,y+12,y+7},7);color(g,VIOLET,1);g.fillPolygon(new int[]{x-11,x-4,x+9,x+10,x+4,x-9},new int[]{y-7,y-10,y-4,y+6,y+9,y+5},6);line(g,x-11,y-7,x-4,y-10,GOLD,2,1);line(g,x-4,y-10,x+9,y-4,GOLD,2,1);for(int i=0;i<3;i++)line(g,x-6+i*5,y-8+i*2,x-7+i*5,y-3+i*2,INK,2,1);color(g,VIOLET,1);g.fillRect(x+9,y+1,6,6);line(g,x-6,y-2,x-4,y+4,CYAN,2,1);line(g,x-4,y+4,x+6,y+6,CYAN,2,1);}
    void above(Graphics2D source,GuardianDraft b,int cx,int cy,boolean reduced){if(!b.alive())return;var g=(Graphics2D)source.create();g.scale(2,2);boolean active=b.state()==Guardian.State.SLAM,preparing=b.state()==Guardian.State.TELEGRAPH;double x=(b.x()-cx)/2,y=(b.y()+Player.COLLISION_Y_OFFSET-cy)/2;
        if(active&&b.move()==GuardianDraft.Move.BACKHAND){double angle=b.angle()-Math.PI/2+clamp(b.stateSeconds()/.3)*Math.PI;var shape=new java.awt.Polygon();for(double a=angle-.7;a<=angle+.01;a+=.07)shape.addPoint((int)(x+Math.cos(a)*91),(int)(y+Math.sin(a)*91-12));for(double a=angle;a>=angle-.71;a-=.07)shape.addPoint((int)(x+Math.cos(a)*67),(int)(y+Math.sin(a)*67-12));color(g,VIOLET,.7);g.fillPolygon(shape);for(double a=angle-.7;a<angle;a+=.07)line(g,x+Math.cos(a)*91,y+Math.sin(a)*91-12,x+Math.cos(a+.07)*91,y+Math.sin(a+.07)*91-12,CYAN,2,.9);}
        if((active||preparing)&&b.move()==GuardianDraft.Move.DETACHED_FISTS)for(int i=0;i<2;i++){double age=active?b.stateSeconds()-i*.48:-.2;if(age>.36)continue;double q=active?clamp(age/.2):0;q=q*q*(3-2*q);double ox=x+(i==0?-36:36),oy=y-45,tx=((i==0?b.firstX():b.secondX())-cx)/2,ty=((i==0?b.firstY():b.secondY())+Player.COLLISION_Y_OFFSET-cy)/2-10,fx=ox+(tx-ox)*q,fy=oy+(ty-oy)*q;if(!reduced)line(g,ox,oy,fx,fy,CYAN,2,.2);fist(g,(int)fx,(int)fy);}
        if((active||preparing)&&b.move()==GuardianDraft.Move.CORE_BEAM){double[] core=core(b);double sx=(core[0]-cx)/2,sy=(core[1]-cy)/2,a=active?b.beamAngle():b.angle(),reach=b.beamReach()/2,ex=x+Math.cos(a)*reach,ey=y+Math.sin(a)*reach-12;
            if(preparing){int radius=4+(int)(clamp(b.stateSeconds()/1.25)*6);color(g,CYAN,.7);g.fillOval((int)sx-radius,(int)sy-radius,radius*2,radius*2);}
            else{line(g,sx,sy,ex,ey,VIOLET,18,.65);line(g,sx,sy,ex,ey,CYAN,9,.9);line(g,sx,sy,ex,ey,WHITE,3,.95);}
        }g.dispose();
    }
}
