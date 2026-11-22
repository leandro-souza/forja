package jakarta.persistence; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) public @interface Id { GenerationType strategy() default GenerationType.IDENTITY; }
