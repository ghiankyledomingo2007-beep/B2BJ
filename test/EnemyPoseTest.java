import java.util.Arrays;

/** Enemy bodies act out their attacks, on the 2 px art grid. */
public final class EnemyPoseTest {
    public static void main(String[] args) {
        for(var role:Wisp.Role.values())for(var state:Wisp.State.values())for(double t=0;t<1.5;t+=.013) {
            double[] p=EnemyPose.pose(role,state,t,.8,.3,.6,-.8,-30,40,t*97);
            assert p[0]%2==0&&p[1]%2==0 : "offsets stay on the 2 px art grid";
            assert Math.abs(p[2]*96/4-Math.round(p[2]*96/4))<1e-9&&Math.abs(p[3]*96/4-Math.round(p[3]*96/4))<1e-9
                    : "a 96 px body grows and shrinks in 4 px steps";
            assert p[2]>.7&&p[2]<1.3&&p[3]>.7&&p[3]<1.3;
        }
        double[] charged=EnemyPose.pose(Wisp.Role.SPITTER,Wisp.State.TELEGRAPH,.79,.8,.3,1,0,0,0,0);
        assert charged[2]>1&&charged[3]<1 : "a charging spitter inflates and squashes down";
        double[] fired=EnemyPose.pose(Wisp.Role.SPITTER,Wisp.State.LUNGE,0,.8,.3,1,0,0,0,0);
        assert fired[0]<0&&fired[3]<1 : "firing kicks it back, squashed";
        double[] crouch=EnemyPose.pose(Wisp.Role.THORN_WOLF,Wisp.State.TELEGRAPH,.79,.8,.3,1,0,0,0,0);
        assert crouch[0]<0&&crouch[3]<1 : "the wolf crouches back before the lunge";
        double[] lunge=EnemyPose.pose(Wisp.Role.THORN_WOLF,Wisp.State.LUNGE,.02,.8,.3,1,0,0,0,0);
        assert lunge[2]>1&&lunge[0]>0 : "the wolf stretches into the lunge";
        assert EnemyPose.pose(Wisp.Role.SHIELD_GUARD,Wisp.State.LUNGE,.1,.8,.3,1,0,0,0,0)[0]>0 : "a guard steps into its swing";
        assert EnemyPose.pose(Wisp.Role.REMNANT,Wisp.State.HURT,0,.8,.3,1,0,-50,0,0)[0]<0 : "a hit pushes the body away";
        assert Arrays.equals(EnemyPose.pose(Wisp.Role.SPITTER,Wisp.State.PATROL,.3,.8,.3,1,0,0,0,0),new double[]{0,0,1,1});
        double[] stride=EnemyPose.pose(Wisp.Role.THORN_WOLF,Wisp.State.PURSUE,0,.8,.3,1,0,0,0,12);
        double[] planted=EnemyPose.pose(Wisp.Role.THORN_WOLF,Wisp.State.PURSUE,0,.8,.3,1,0,0,0,24);
        assert stride[1]<0&&planted[1]==0&&stride[0]>0 : "a trotting wolf rises on each stride and leans into the chase";
        System.out.println("EnemyPoseTest passed: 2 px grid, charge squash, firing kick, crouch and lunge, step-in, hit recoil, walking step");
    }
}
