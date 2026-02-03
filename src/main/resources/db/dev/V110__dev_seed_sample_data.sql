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
-- SALES + SALE_DETAILS + INVENTORY_MOVEMENTS
-- (solo si sales está vacío)
-- ==============
DO
$$
    DECLARE
        v_customer1 BIGINT;
        v_customer2 BIGINT;

        v_sale1     BIGINT;
        v_sale2     BIGINT;

        v_mov1      BIGINT;
        v_mov2      BIGINT;

        p_cola      BIGINT;
        p_inka      BIGINT;
        p_lays      BIGINT;
        p_leche     BIGINT;

        s_cola      INTEGER;
        s_inka      INTEGER;
        s_lays      INTEGER;
        s_leche     INTEGER;
    BEGIN
        -- customers
        SELECT id INTO v_customer1 FROM public.customers WHERE email = 'juan.perez@test.com';
        SELECT id INTO v_customer2 FROM public.customers WHERE email = 'maria.torres@test.com';

        -- products
        SELECT id, stock INTO p_cola, s_cola FROM public.products WHERE name = 'Coca Cola 500ml';
        SELECT id, stock INTO p_inka, s_inka FROM public.products WHERE name = 'Inka Kola 500ml';
        SELECT id, stock INTO p_lays, s_lays FROM public.products WHERE name = 'Papas Lays 45g';
        SELECT id, stock INTO p_leche, s_leche FROM public.products WHERE name = 'Leche Gloria 1L';

        IF NOT EXISTS (SELECT 1 FROM public.sales) THEN

            -- =========================
            -- SALE #1
            -- =========================
            INSERT INTO public.sales
            (sale_date, status, total_amount, customer_id, deleted, created_at, updated_at)
            VALUES
                (now() - interval '2 days', 'ACTIVE', 9.00, v_customer1, false, now(), now())
            RETURNING id INTO v_sale1;

            INSERT INTO public.sale_details
            (quantity, unit_price, sub_total, product_id, sale_id)
            VALUES
                (2, 3.50, 7.00, p_cola, v_sale1),
                (1, 2.00, 2.00, p_lays, v_sale1);

            -- Inventory movement (OUT)
            INSERT INTO public.inventory_movements
            (movement_type, source_type, source_id, reason, created_at, updated_at)
            VALUES
                ('OUT', 'SALE', v_sale1, 'Venta seed #1', now(), now())
            RETURNING id INTO v_mov1;

            -- Items
            INSERT INTO public.inventory_movement_items
            (movement_id, product_id, quantity, previous_stock, new_stock)
            VALUES
                (v_mov1, p_cola, 2, s_cola, s_cola - 2),
                (v_mov1, p_lays, 1, s_lays, s_lays - 1);

            -- Update stock
            UPDATE public.products SET stock = stock - 2 WHERE id = p_cola;
            UPDATE public.products SET stock = stock - 1 WHERE id = p_lays;

            -- =========================
            -- SALE #2
            -- =========================
            INSERT INTO public.sales
            (sale_date, status, total_amount, customer_id, deleted, created_at, updated_at)
            VALUES
                (now() - interval '1 days', 'ACTIVE', 13.10, v_customer2, false, now(), now())
            RETURNING id INTO v_sale2;

            INSERT INTO public.sale_details
            (quantity, unit_price, sub_total, product_id, sale_id)
            VALUES
                (1, 3.50, 3.50, p_inka, v_sale2),
                (2, 4.80, 9.60, p_leche, v_sale2);

            -- Inventory movement (OUT)
            INSERT INTO public.inventory_movements
            (movement_type, source_type, source_id, reason, created_at, updated_at)
            VALUES
                ('OUT', 'SALE', v_sale2, 'Venta seed #2', now(), now())
            RETURNING id INTO v_mov2;

            -- Items
            INSERT INTO public.inventory_movement_items
            (movement_id, product_id, quantity, previous_stock, new_stock)
            VALUES
                (v_mov2, p_inka, 1, s_inka, s_inka - 1),
                (v_mov2, p_leche, 2, s_leche, s_leche - 2);

            -- Update stock
            UPDATE public.products SET stock = stock - 1 WHERE id = p_inka;
            UPDATE public.products SET stock = stock - 2 WHERE id = p_leche;

        END IF;
    END
$$;
