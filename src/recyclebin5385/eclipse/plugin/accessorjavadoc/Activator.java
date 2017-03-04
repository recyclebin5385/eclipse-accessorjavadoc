package recyclebin5385.eclipse.plugin.accessorjavadoc;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * このプラグインのactivator。
 * 
 * @author owner
 *
 */
public class Activator extends AbstractUIPlugin {
    /**
     * プラグインID
     */
    public static final String PLUGIN_ID = "recyclebin5385.eclipse.plugin.accessorjavadoc";

    /**
     * 共有インスタンス
     */
    private static Activator s_plugin;

    /**
     * コンストラクタ。
     */
    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        s_plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        s_plugin = null;
        super.stop(context);
    }

    /**
     * 共有インスタンスを取得する。
     *
     * @return 共有インスタンス
     */
    public static Activator getDefault() {
        return s_plugin;
    }

    /**
     * 指定されたプラグインからの相対パスに置かれた画像ファイルから {@link ImageDescriptor}を取得する。
     *
     * @param path
     *            パス
     * @return {@link ImageDescriptor}
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
}
