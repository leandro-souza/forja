package jakarta.persistence; public @interface Column { boolean unique() default false; boolean nullable() default true; String columnDefinition() default ""; String name() default ""; }
