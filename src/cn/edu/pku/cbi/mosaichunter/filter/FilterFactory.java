/*
 * The MIT License
 *
 * Copyright (c) 2016 Center for Bioinformatics, Peking University
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package cn.edu.pku.cbi.mosaichunter.filter;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class FilterFactory {

    public static Filter create(String name) throws Exception {
        if (name == null) {
            throw new IllegalArgumentException("filter name is null.");
        }
        String className = ConfigManager.getInstance().get(name, "class");
        if (className == null || className.trim().isEmpty()) {
            throw new Exception(name + ".class property is missing");
        }
        
        try {
            Filter filter = (Filter) 
                    Class.forName(className).getConstructor(String.class).newInstance(name);
            return filter;
        } catch (Exception e) {
            throw new Exception("cannot create class " + className + "(" + name + ")", e);
        }
        
    }
    
    public static Filter[] create(String[] names) throws Exception {
        
        if (names == null) {
            return new Filter[0]; 
        }        
        Filter[] instances = new Filter[names.length];
        for (int i = 0; i < instances.length; ++i) {
            instances[i] = create(names[i]);
        }
        return instances;        
    }
    
}
