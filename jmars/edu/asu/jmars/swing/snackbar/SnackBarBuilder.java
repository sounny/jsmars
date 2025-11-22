package edu.asu.jmars.swing.snackbar;

import mdlaf.utils.MaterialFontFactory;
import javax.swing.*;
import org.material.component.swingsnackbar.SnackBar;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SnackBarBuilder{

    private static Map<UUID, SnackBar> cache = new HashMap<>();

    public static SnackBar build(Window own, String contentText, String textIcon, UUID uuid){
        if(own == null){
            throw new IllegalArgumentException("Snackbar owner window is null");
        }
        if(cache.containsKey(uuid)){        	
            return cache.get(uuid).setText(contentText);
        }     
        SnackBar newSnackbar = JMarsSnackBar.make(own, contentText, textIcon)
                .setIconTextColor(ThemeSnackBar.getForegroundStandard())
                .setIconTextStyle(MaterialFontFactory.getInstance().getFont(
                        MaterialFontFactory.BOLD
                ))
                .setDuration(SnackBar.LENGTH_LONG);
        cache.put(uuid, newSnackbar);
        return newSnackbar;
    }

    public static SnackBar build(Window own, String contentText, Icon icon, Icon leftIcon, UUID uuid){
        if(own == null){
            throw new IllegalArgumentException("Snackbar owner window is null");
        }
        if(cache.containsKey(uuid)){           	
            return cache.get(uuid).setText(contentText);
        }          
        SnackBar newSnackbar = JMarsSnackBar.make(own, contentText, icon).setLeftIcon(leftIcon);     
        cache.put(uuid, newSnackbar);
        return newSnackbar;
    }
 
    public static SnackBar build(Window own, String contentText, Icon icon, UUID uuid){
        if(own == null){
            throw new IllegalArgumentException("Snackbar owner window is null");
        }
        if(cache.containsKey(uuid)){           	
            return cache.get(uuid).setText(contentText);
        }          
        SnackBar newSnackbar = JMarsSnackBar.make(own, contentText, icon);      
        cache.put(uuid, newSnackbar);
        return newSnackbar;
    }    
    
    
    public static SnackBar build(Window own, String contentText, UrlLabel link, UUID uuid){
        if(own == null){
            throw new IllegalArgumentException("Snackbar owner window is null");
        }
        if(cache.containsKey(uuid)){           	
            return cache.get(uuid).setText(contentText);
        }          
        SnackBar newSnackbar = JMarsSnackBar.make(own, contentText, link);      
        cache.put(uuid, newSnackbar);
        return newSnackbar;
    }    
    
    
    public static SnackBar getSnackBarOn(UUID uuid){   	
        if(uuid == null){
            throw new IllegalArgumentException("Snackbar owner window is null");
        }
        if(cache.containsKey(uuid)){        	
            return cache.get(uuid);
        }
        return null;
    }

    public static void invalidateSnackBar(UUID uuid){
        if(uuid == null){
            throw new IllegalArgumentException("Snackbar owner window is null");
        }
        if(cache.containsKey(uuid)){       	
            SnackBar snackBar = cache.get(uuid);
            snackBar.dismiss();
            cache.remove(uuid);
        }
    }

}
