package jakarta.persistence; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) public @interface Entity { GenerationType strategy() default GenerationType.IDENTITY; }
