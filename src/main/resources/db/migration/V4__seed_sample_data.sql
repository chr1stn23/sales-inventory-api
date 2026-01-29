DO
$$
    BEGIN
        -- ==============
        -- CATEGORIES
        -- ==============
        IF NOT EXISTS (SELECT 1 FROM public.categories) THEN
            INSERT INTO public.categories (name, description, deleted, created_at, updated_at)
            VALUES ('Bebidas', 'Bebidas gaseosas y jugos', false, now(), now()),
                   ('Snacks', 'Snacks y galletas', false, now(), now()),
                   ('Lácteos', 'Productos lácteos', false, now(), now()),
                   ('Limpieza', 'Productos de limpieza', false, now(), now());
        END IF;

        -- ==============
        -- CUSTOMERS
        -- ==============
        IF NOT EXISTS (SELECT 1 FROM public.customers) THEN
            INSERT INTO public.customers (full_name, email, deleted, created_at, updated_at)
            VALUES ('Juan Pérez', 'juan.perez@test.com', false, now(), now()),
                   ('María Torres', 'maria.torres@test.com', false, now(), now()),
                   ('Carlos Rojas', 'carlos.rojas@test.com', false, now(), now());
        END IF;
    END
$$;


-- ==============
-- PRODUCTS (con FK a category)
-- ==============
DO
$$
    DECLARE
        v_bebidas  BIGINT;
        v_snacks   BIGINT;
        v_lacteos  BIGINT;
        v_limpieza BIGINT;
    BEGIN
        SELECT id INTO v_bebidas FROM public.categories WHERE name = 'Bebidas';
        SELECT id INTO v_snacks FROM public.categories WHERE name = 'Snacks';
        SELECT id INTO v_lacteos FROM public.categories WHERE name = 'Lácteos';
        SELECT id INTO v_limpieza FROM public.categories WHERE name = 'Limpieza';

        -- Inserta solo si no hay productos aún
        IF NOT EXISTS (SELECT 1 FROM public.products) THEN
            INSERT INTO public.products (name, description, price, stock, category_id, deleted, created_at, updated_at)
            VALUES
                -- Bebidas
                ('Coca Cola 500ml', 'Gaseosa personal', 3.50, 48, v_bebidas, false, now(), now()),
                ('Inka Kola 500ml', 'Gaseosa personal', 3.50, 25, v_bebidas, false, now(), now()),

                -- Snacks
                ('Papas Lays 45g', 'Snack clásico', 2.00, 95, v_snacks, false, now(), now()),
                ('Galletas Oreo', 'Paquete 6u', 4.20, 60, v_snacks, false, now(), now()),

                -- Lácteos
                ('Leche Gloria 1L', 'Leche entera', 4.80, 40, v_lacteos, false, now(), now()),
                ('Yogurt fresa', 'Vaso 180g', 2.50, 30, v_lacteos, false, now(), now()),

                -- Limpieza
                ('Detergente 1kg', 'Multiusos', 8.90, 20, v_limpieza, false, now(), now()),
                ('Lejía 1L', 'Desinfectante', 4.50, 15, v_limpieza, false, now(), now());
        END IF;
    END
$$;


-- ==============
-- SALES + SALE_DETAILS
-- (solo si sales está vacío)
-- ==============
DO
$$
    DECLARE
        v_customer1 BIGINT;
        v_customer2 BIGINT;
        v_sale1     BIGINT;
        v_sale2     BIGINT;
        p_cola      BIGINT;
        p_inka      BIGINT;
        p_lays      BIGINT;
        p_leche     BIGINT;
    BEGIN
        -- customers
        SELECT id INTO v_customer1 FROM public.customers WHERE email = 'juan.perez@test.com';
        SELECT id INTO v_customer2 FROM public.customers WHERE email = 'maria.torres@test.com';

        -- products
        SELECT id INTO p_cola FROM public.products WHERE name = 'Coca Cola 500ml';
        SELECT id INTO p_inka FROM public.products WHERE name = 'Inka Kola 500ml';
        SELECT id INTO p_lays FROM public.products WHERE name = 'Papas Lays 45g';
        SELECT id INTO p_leche FROM public.products WHERE name = 'Leche Gloria 1L';

        IF NOT EXISTS (SELECT 1 FROM public.sales) THEN
            -- Venta #1 (para probar void/anular)
            -- Detalles:
            -- 2 x Coca Cola 500ml @ 3.50 = 7.00
            -- 1 x Papas Lays 45g  @ 2.00 = 2.00
            -- TOTAL = 9.00
            INSERT INTO public.sales (sale_date, total_amount, customer_id, deleted, created_at, updated_at)
            VALUES (now() - interval '2 days', 9.00, v_customer1, false, now(), now())
            RETURNING id INTO v_sale1;

            INSERT INTO public.sale_details (quantity, unit_price, sub_total, product_id, sale_id)
            VALUES (2, 3.50, 7.00, p_cola, v_sale1),
                   (1, 2.00, 2.00, p_lays, v_sale1);

            -- Venta #2 (más data para filtros/paginación)
            -- 1 x Inka Kola 500ml @ 3.50 = 3.50
            -- 2 x Leche Gloria 1L @ 4.80 = 9.60
            -- TOTAL = 13.10
            INSERT INTO public.sales (sale_date, total_amount, customer_id, deleted, created_at, updated_at)
            VALUES (now() - interval '1 days', 13.10, v_customer2, false, now(), now())
            RETURNING id INTO v_sale2;

            INSERT INTO public.sale_details (quantity, unit_price, sub_total, product_id, sale_id)
            VALUES (1, 3.50, 3.50, p_inka, v_sale2),
                   (2, 4.80, 9.60, p_leche, v_sale2);

        END IF;
    END
$$;
