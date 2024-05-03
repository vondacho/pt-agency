/*-
 * #%L
 * pta-booking
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
// This config is here because there is already an index in META-INF/resources/index.html
// If you want this as the index, remove META-INF/resources/
// Then you can remove this config file and rename quinoa.html to index.html
import { resolve } from 'path'
import { defineConfig } from 'vite'
import express from 'express'

const app = express()
app.get('/', (req, res) => {
    res.send('Allow detection by Quinoa').end();
})

function expressPlugin() {
    return {
        name: 'express-plugin',
        configureServer(server) {
            server.middlewares.use(app);
        }
    }
}

export default defineConfig({
    plugins: [expressPlugin()],
    build: {
        rollupOptions: {
            input: {
                quinoa: resolve(__dirname, 'quinoa.html'),
            },
        },
    },
})
