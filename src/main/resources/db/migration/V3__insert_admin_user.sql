DO
$$
    DECLARE
        v_user_id BIGINT;
    BEGIN
        -- 1) crear usuario si no existe
        INSERT INTO users (email, password_hash, enabled, created_at)
        VALUES ('admin@local.com',
                '$2a$10$UinI/Acwt4VWAX5xnjQBPuYWQ4zNCKfL59MD9J5c/OnPUy16RLWei',
                true,
                now())
        ON CONFLICT (email) DO NOTHING;

        -- 2) obtener id (ya sea recién creado o existente)
        SELECT id
        INTO v_user_id
        FROM users
        WHERE email = 'admin@local.com';

        -- 3) insertar rol si no existe
        INSERT INTO user_roles (user_id, role)
        VALUES (v_user_id, 'ADMIN')
        ON CONFLICT DO NOTHING;
    END
$$;
