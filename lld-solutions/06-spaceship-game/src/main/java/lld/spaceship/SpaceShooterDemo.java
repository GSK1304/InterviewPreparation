package lld.spaceship;

public class SpaceShooterDemo {
    public static void main(String[] args) throws InterruptedException {
        SpaceShooterGame game = new SpaceShooterGame();
        game.start();
        game.render();

        for (int i = 0; i < 25 && game.getStatus() == GameStatus.PLAYING; i++) {
            game.playerShoot();
            if (i < 8)       game.movePlayer(Direction.LEFT);
            else if (i < 16) game.movePlayer(Direction.RIGHT);
            game.tick();
            if (i % 5 == 0) game.render();
            Thread.sleep(GameConfig.TICK_MS);
        }

        System.out.println("Final score: " + game.getScore());
        System.out.println("Reached level: " + game.getLevel());
        System.out.println("Status: " + game.getStatus());
    }
}
