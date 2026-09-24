package dev.jco.upgrades;
public final class Accelerator {
 public final StackMatcher item;public final int actions,reduction;private int damage,consume,cooldown=10;private boolean repeat=false,frozen;private final Feedback feedback=new Feedback();
 public Accelerator(String item,int actions,int reduction,int damage,int consume){this.item=new StackMatcher(item);if(actions<1||actions>1000000||reduction<1||reduction>1000000000)throw new IllegalArgumentException("Invalid accelerator");this.actions=actions;this.reduction=reduction;this.damage=damage;this.consume=consume;}
 private InteractionMode input=InteractionMode.RIGHT;private boolean grouped;public Accelerator input(String value){edit();input=InteractionMode.parse(value);return this;}public InteractionMode input(){return input;}public Accelerator grouped(boolean value){edit();grouped=value;return this;}public boolean grouped(){return grouped;}
 private void edit(){if(frozen)throw new IllegalStateException("Frozen accelerator");}
 public Accelerator durability(int n){edit();if(n<0||n>1000000)throw new IllegalArgumentException();damage=n;return this;}public int damage(){return damage;}
 public Accelerator consume(int n){edit();if(n<0||n>64)throw new IllegalArgumentException();consume=n;return this;}public int consume(){return consume;}
 public Accelerator actionCooldown(int n){edit();if(n<0||n>12000)throw new IllegalArgumentException();cooldown=n;return this;}public int cooldown(){return cooldown;}
 public Accelerator repeatable(boolean value){edit();repeat=value;return this;}public boolean repeatable(){return repeat;}
 public Accelerator feedback(java.util.function.Consumer<Feedback> c){edit();c.accept(feedback);return this;}public Feedback feedback(){return feedback;}
 public void freeze(){feedback.freeze();frozen=true;}
 public String fingerprint(){return item+":"+actions+":"+reduction+":"+damage+":"+consume+":"+repeat;}
}
